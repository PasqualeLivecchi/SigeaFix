package sigea.main;

import sigea.entities.BatchOfMsgReadings;
import sigea.entities.HealthReading;
import sigea.entities.MsgQuality;
import sigea.entities.HealthStatus;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.disposables.SerialDisposable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * * * * * @author Pasquale Livecchi
 */
@Slf4j
public abstract class MsgAcquisition {

// qualities that are not bad or uncertain 
    private static final EnumSet<MsgQuality> OK_QUALITIES = EnumSet.of(MsgQuality.GOOD, MsgQuality.NONE);

    @Inject
    private Event<BatchOfMsgReadings> sendData;
    @Inject
    private Event<HealthReading> sendHealth;
    @Inject
    protected Scheduler computation;
    @Inject
    @Io
    protected Scheduler io;

// packages together all reading streams under a MsgType 
// only allows one subscription to be active at a time 
    protected class MsgReadingSubscription {

        private final SerialDisposable eventTasks = new SerialDisposable();

        public MsgReadingSubscription() {
        }

        // sets up the event streams if readings contains a value
        public synchronized void updateEventStream(Option<Seq<Observable<BatchOfMsgReadings>>> readings) {
            if (readings.isDefined()) {
                eventTasks.set(createCombinedSubscription(readings.get()));
            } else {
                eventTasks.set(Disposables.empty());
            }
        }

        // bind all reading streams and health stream to a single subscription
        protected CompositeDisposable createCombinedSubscription(Seq<Observable<BatchOfMsgReadings>> batchOfMsgReadings) {
            // setup health recording
            Disposable health = Observable.merge(batchOfMsgReadings)
                    .compose(analyzeBatchHealth())
                    .doOnError(ex -> log.error("Error in health stream", ex))
                    .retry()
                    // add startup and shutdown logging
                    .doOnSubscribe(s -> log.info("Starting Up Sigea Msg Acquisition"))
                    .doOnDispose(() -> log.info("Shutting Down Sigea Msg Acquisition"))
                    .subscribe(sendHealth::fire);
            Seq<Disposable> subs = batchOfMsgReadings
                    .map(data -> data.subscribe(sendData::fire));

            return new CompositeDisposable(subs.prepend(health).toArray());
        }

        // returns true only if there is at least one value that does not
        // have bad quality
        private boolean isOkQuality(BatchOfMsgReadings bomr) {
            if (bomr == null) {
                return false;
            }
            return bomr.getMsgReadings().stream()
                    .map(r -> MsgQuality.fromQualityByte(r.getQuality()))
                    .anyMatch(OK_QUALITIES::contains);
        }

        // split a stream into two based on a predicate
        private <T> Tuple2<Observable<T>, Observable<T>> splitStreamFilter(Observable<T> stream, Predicate<T> predicate) {
            return Tuple.of(stream.filter(predicate::test), stream.filter(predicate.negate()::test));
        }

        private void addHealthMsg(HealthReading health) {
            switch (health.getCode()) {
                case NOT_OPERATIONAL:
                case PARTIALLY_OPERATIONAL:
                    health.setMessage("Sigea Msg Acquisition is not receiving all expected data");
                    break;
                case OPERATIONAL:
                    health.setMessage("Sigea Msg Acquisition is operating normally");
                    break;
                default:
                    throw new RuntimeException("Invalid Msg health code");
            }
        }

        // creates a status based on good vs bad readings
        private HealthStatus calculateStatusCode(long good, long bad) {
            if (good > 0) {
                if (bad == 0) {
                    return HealthStatus.OPERATIONAL;
                } else {
                    return HealthStatus.PARTIALLY_OPERATIONAL;
                }
            } else {
                return HealthStatus.NOT_OPERATIONAL;
            }
        }

        // this analyzes a BatchOfMsgReadings stream in 30 second increments
        // and produces a HealthReading based on the readings encountered
        private ObservableTransformer<BatchOfMsgReadings, HealthReading> analyzeBatchHealth() {
            return readings -> {
                return splitStreamFilter(readings, this::isOkQuality)
                        // create a 30 second windowed observable for counting
                        .map1(obs -> obs.window(30, TimeUnit.SECONDS, computation))
                        .map2(obs -> obs.window(30, TimeUnit.SECONDS, computation))
                        .apply((goodwindows, badwindows) -> {
                            // 30 second windows get zipped together
                            return goodwindows.zipWith(badwindows,
                                    // and their counts get analyzed by status(good, bad)
                                    (good, bad) -> good.count().zipWith(bad.count(), this::calculateStatusCode))
                                    // flatten out the stream
                                    // then add timestamp to build HealthReading
                                    .flatMap(obs -> obs.toObservable())
                                    .timestamp(computation)
                                    .map(code -> {
                                        HealthReading health = new HealthReading();
                                        health.setCode(code.value());
                                        health.setTime(code.time());
                                        addHealthMsg(health);
                                        return health;
                                    });
                        });
            };
        }
    }
// when an error occurs emit the specified value before the error

    protected <T> ObservableTransformer<T, T> onErrorInsert(T err) {
        return obs -> obs.onErrorResumeNext(th -> {
            return Observable.just(err).concatWith(Observable.error(th));
        });

    }

    protected interface ObservableMsgInterface {

        Observable<BatchOfMsgReadings> asObservable();
    }

    protected <M, R> void loadBatchOfMsgReadings(
            List<M> messages,
            MsgReadingSubscription msgSub,
            Function<M, R> configMapper,
            BiFunction<R, Seq<M>, ObservableMsgInterface> combineConfigWithSensors) {
        Option<Seq<Observable<BatchOfMsgReadings>>> obsReadingBatch = Option.of(messages)
                .filter(msgs -> !msgs.isEmpty())
                .map(filteredMsgs -> {
                    return Stream.ofAll(filteredMsgs)
                            .groupBy(msg -> configMapper.apply(msg))
                            .map(tup -> tup.apply(combineConfigWithSensors)
                            .asObservable())
                            .toList();
                });
        msgSub.updateEventStream(obsReadingBatch);
    }
}

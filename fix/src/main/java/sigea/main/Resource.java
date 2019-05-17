package sigea.main;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;

public interface Resource<T> {

    /**
     * * Gets the next value from this resource * @return * @throws Exception
     */
    T next() throws Exception;

    /**
     * Closes any open objects associated with this resource
     *
     * @throws Exception
     */
    void dispose() throws Exception;

    /**
     * Continuously calls the next method to emit an observable stream
     *
     * @param <T>
     * @param func
     * @return
     */
    static <T> Observable<T> continuousResourceStream(CheckedFunction0<Resource<T>> func) {
        return Observable.create(emitter -> {
            try {
                Resource<T> syncResource = func.apply();

                Disposable continuousStream = Observable.just(1)
                        .repeat()
                        .doOnDispose(() -> Try.run(syncResource::dispose))
                        .subscribe(i -> Try.of(syncResource::next)
                        .onFailure(emitter::onError)
                        .onSuccess(emitter::onNext));
                emitter.setDisposable(continuousStream);
            } catch (Throwable ex) {
                emitter.onError(ex);
            }
        });
    }
}

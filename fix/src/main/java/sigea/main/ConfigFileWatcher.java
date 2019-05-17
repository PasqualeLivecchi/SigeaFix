package sigea.main;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Implements file watching based on java.nio.file and rx. Events are polled
 * every 1 second.
 *
 * @author Pasquale Livecchi
 */
public class ConfigFileWatcher {

    private final Scheduler computation;
    private final Observable<Tuple2<WatchEvent.Kind<?>, Path>> fileEventStream;

    /**
     *
     * @param file - the file/folder to watch
     * @param computation
     */
    public ConfigFileWatcher(Path file, Scheduler computation) {
        this.computation = computation;
        fileEventStream = Observable.using(() -> createWatchService(file),
                this::createEventStreamOnFileChange,
                this::closeWatchService)
                .retryWhen(err -> err.delay(15, TimeUnit.SECONDS, computation))
                .share();
    }

    /**
     * Creates an observable file event stream that polls for file modification
     * events
     *
     * @return
     */
    public Observable<Tuple2<WatchEvent.Kind<?>, Path>> createFileEventStream() {
        return fileEventStream;
    }

    private WatchService createWatchService(Path folder) {
        return Try.of(() -> {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            folder.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            return watcher;
        }).get();
    }

    private Observable<Tuple2<WatchEvent.Kind<?>, Path>> createEventStreamOnFileChange(WatchService watcher) {
        return Observable.fromCallable(watcher::poll)
                .delay(1, TimeUnit.SECONDS, computation)
                .repeat()
                .filter(Objects::nonNull)
                .flatMapIterable(key -> {
                    List<WatchEvent<?>> events = key.pollEvents();
                    key.reset();
                    return events;
                })
                .map(event -> Tuple.of(event.kind(), (Path) event.context()));
    }

    private void closeWatchService(WatchService watcher) {
        Try.run(watcher::close);
    }
}


package nz.net.ultraq.web.filter;



import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A resource that is a file in the file system.
 * 
 * @author Emanuel Rabina
 */
public class ResourceFile implements Resource {

	protected static final ExecutorService resourceWatchingExecutorService = Executors.newCachedThreadPool();

	protected final Path resourcefile;
	protected boolean modified;
	protected String sourcecontent;
	protected String processedcontent;

	/**
	 * Constructor, get a handle to the file on the given path and set up a
	 * watch service so that changes to the file can be monitored.
	 * 
	 * @param path Path to the file on the file system.
	 * @throws IOException If there was a problem registering the watch service.
	 */
	public ResourceFile(String path) throws IOException {

		FileSystem filesystem = FileSystems.getDefault();
		resourcefile = filesystem.getPath(path);

		watchFile(filesystem, resourcefile, new FileModifiedCallback() {
			@Override
			public void fileModified() {
				modified = true;
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProcessedContent() {

		return processedcontent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSourceContent() {

		return sourcecontent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isModified() {

		return modified;
	}

	/**
	 * Register a 'file modified' watch service on the given file, invoking the
	 * callback if the file modification event has been triggered.
	 * 
	 * @param filesystem
	 * @param file
	 * @param callback
	 * @throws IOException If there was a problem registering the watch service.
	 */
	protected void watchFile(FileSystem filesystem, final Path file, final FileModifiedCallback callback)
		throws IOException {

		// NOTE: Can we watch just a file for modification?  All the examples online
		//       show watching a directory, then checking the context for the file.

		Path resourcedir  = resourcefile.getParent();
		final WatchService watchservice = filesystem.newWatchService();
		resourcedir.register(watchservice, StandardWatchEventKinds.ENTRY_MODIFY);

		resourceWatchingExecutorService.submit(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {

				try {
					boolean valid = true;
					while (valid) {
						WatchKey key = watchservice.take();
						for (WatchEvent<?> event: key.pollEvents()) {
							if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
								continue;
							}

							// Check if this modification event is for the resource we are watching
							Path modifiedfile = ((WatchEvent<Path>)event).context();
							if (modifiedfile.equals(file)) {
								callback.fileModified();
							}
						}
						valid = key.reset();
					}
				}
				catch (InterruptedException ex) {
					return;
				}
			}
		});
	}

	/**
	 * Callback to be notified of file modification events when registered using
	 * {@link ResourceFile#watchFile(FileSystem, Path, FileModifiedCallback)}.
	 */
	protected static interface FileModifiedCallback {

		/**
		 * Invoked when a file registered to be watched has been modified.
		 */
		public void fileModified();
	}
}

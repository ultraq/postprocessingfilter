
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
 * Representation of a resource in both its initial and processed states.
 * 
 * @author Emanuel Rabina
 */
public class Resource {

	protected static final ExecutorService resourceWatchingExecutorService = Executors.newCachedThreadPool();

	protected final Path resource;
	protected String sourcecontent;
	protected String processedcontent;
	protected boolean modified;

	/**
	 * Constructor, get a handle to the resource on the given path and set up a
	 * watch service so that changes to the resource can be monitored.
	 * 
	 * @param path Path to the resource.
	 * @throws IOException If there was a problem registering the watch service.
	 */
	public Resource(String path) throws IOException {

		FileSystem filesystem = FileSystems.getDefault();
		resource = filesystem.getPath(path);
		watchResource(resource);
	}

	/**
	 * Constructor, same as {@link #Resource(String)} but also sets the content
	 * of the file from the response capture.
	 * 
	 * @param path			Path to the file on the file system.
	 * @param sourcecontent Response wrapper used to capture the resource file.
	 * @throws IOException If there was a problem registering the watch service.
	 */
	public Resource(String path, String sourcecontent) throws IOException {

		this(path);
		this.sourcecontent = sourcecontent;
	}

	/**
	 * Return the resource's name.
	 * 
	 * @return Resource name.
	 */
	public String getFilename() {

		return resource.getFileName().toString();
	}

	/**
	 * Return the content of the resource after it has been processed.
	 * 
	 * @return Processed resource, or <tt>null</tt> if the resource has not yet
	 * 		   been processed.
	 */
	public String getProcessedContent() {

		return processedcontent;
	}

	/**
	 * Return the content of the resource before it has been processed.
	 * 
	 * @return Original resource content.
	 */
	public String getSourceContent() {

		return sourcecontent;
	}

	/**
	 * Return whether or not the resource has been modified since the last time
	 * it was processed.
	 * 
	 * @return <tt>true</tt> if the resource has changed, <tt>false</tt>
	 * 		   otherwise.
	 */
	public boolean isModified() {

		return modified;
	}

	/**
	 * Set the processed content of the resource.
	 * 
	 * @param processedcontent
	 */
	public void setProcessedContent(String processedcontent) {

		this.processedcontent = processedcontent;
	}

	/**
	 * Register a 'file modified' watch service on the given resource, invoking
	 * the callback if the file modification event has been triggered.
	 * 
	 * @param resource
	 * @throws IOException If there was a problem registering the watch service.
	 */
	protected void watchResource(final Path resource) throws IOException {

		Path resourcedir  = resource.getParent();
		final WatchService watchservice = resource.getFileSystem().newWatchService();
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
							Path modifiedresource = ((WatchEvent<Path>)event).context();
							if (modifiedresource.equals(resource)) {
								modified = true;
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
}

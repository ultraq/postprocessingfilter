
package nz.net.ultraq.web.filter;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Representation of a resource in both its initial and processed states.
 * 
 * @author Emanuel Rabina
 */
public class Resource {

	private static final ExecutorService resourcewatchingexecutorservice = Executors.newCachedThreadPool();
	private static final ArrayList<WatchService> watchservices = new ArrayList<>();

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
	 * Called by the post-processing filter on cleanup, stops all resource
	 * watching threads.
	 */
	static void stopWatchServices() {

		// Close all watch services
		for (WatchService watchservice: watchservices) {
			try {
				watchservice.close();
			}
			catch (IOException ex) {
				// Do nothing with it
			}
		}

		// Close all lingering modification threads
		resourcewatchingexecutorservice.shutdown();
		try {
			if (!resourcewatchingexecutorservice.awaitTermination(5, TimeUnit.SECONDS)) {
				resourcewatchingexecutorservice.shutdownNow();
			}
		}
		catch (InterruptedException ex) {
			// Do nothing with it
		}
	}

	/**
	 * Watch the given resource for changes, causing this resource object to be
	 * marked as 'modified' when the resource is modified.
	 * 
	 * @param resourcefile
	 * @throws IOException If there was a problem watching the resource.
	 */
	protected void watchResource(final Path resourcefile) throws IOException {

		final Path resourcedir = resourcefile.getParent();
		final WatchService watchservice = resourcedir.getFileSystem().newWatchService();
		watchservices.add(watchservice);
		resourcedir.register(watchservice, StandardWatchEventKinds.ENTRY_MODIFY);

		resourcewatchingexecutorservice.submit(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {
				Thread.currentThread().setName("Resource modification listener for directory - " +
						resourcedir.getFileName());

				try {
					while (true) {
						WatchKey key = watchservice.take();
						for (WatchEvent<?> event: key.pollEvents()) {
							if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
								continue;
							}

							// Check if this modification event is for a resource we are watching
							Path modifiedresource = ((WatchEvent<Path>)event).context().getFileName();
							if (resourcefile.getFileName().equals(modifiedresource)) {
								modified = true;

								// Stop listening for modifications as a new resource will be created
								// with new listeners to take the place of these ones
								watchservice.close();
								watchservices.remove(watchservice);
								return;
							}
						}
						boolean valid = key.reset();
						if (!valid) {
							break;
						}
					}
				}
				// Do nothing on thread death/interruption events
				catch (InterruptedException ex) {
				}
				catch (IOException ex) {
				}
				catch (ClosedWatchServiceException ex) {
				}
			}
		});
	}
}

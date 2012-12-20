/*
 * Copyright 2012, Emanuel Rabina (http://www.ultraq.net.nz/)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nz.net.ultraq.web.filter;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Representation of a resource in both its initial and processed states.
 * 
 * @author Emanuel Rabina
 */
public class Resource {

	private static final ExecutorService resourcewatchingservice = Executors.newCachedThreadPool();
	static {
		// Close all lingering modification threads on shutdown
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				resourcewatchingservice.shutdown();
				try {
					if (!resourcewatchingservice.awaitTermination(5, TimeUnit.SECONDS)) {
						resourcewatchingservice.shutdownNow();
					}
				}
				catch (InterruptedException ex) {
					// Do nothing with it
				}
			}
		});
	}

	protected final Path resource;
	protected String sourcecontent;
	protected String processedcontent;
	protected boolean modified;

	/**
	 * Constructor, get a handle to the resource on the given path and set up a
	 * watch service so that changes to the resource can be monitored.
	 * 
	 * @param path Path to the resource.
	 */
	public Resource(String path) {

		resource = Paths.get(path);
		watchResource(resource);
	}

	/**
	 * Constructor, same as {@link #Resource(String)} but also sets the content
	 * of the file from the response capture.
	 * 
	 * @param path			Path to the file on the file system.
	 * @param sourcecontent Response wrapper used to capture the resource file.
	 */
	public Resource(String path, String sourcecontent) {

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
	 * Watch the given resource for changes, causing this resource object to be
	 * marked as 'modified' when the resource is modified.
	 * 
	 * @param resourcefile
	 */
	protected void watchResource(final Path resourcefile) {

		resourcewatchingservice.submit(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {

				Path resourcedir = resourcefile.getParent();
				Thread.currentThread().setName("Resource modification listener for directory - " +
						resourcedir.getFileName());

				try (WatchService watchservice = resourcedir.getFileSystem().newWatchService()) {
					resourcedir.register(watchservice, StandardWatchEventKinds.ENTRY_MODIFY);

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
				catch (InterruptedException | IOException | ClosedWatchServiceException ex) {
				}
			}
		});
	}
}

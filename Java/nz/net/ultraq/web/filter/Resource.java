package nz.net.ultraq.web.filter;



/**
 * Representation of a resource in both its initial and processed states.
 * 
 * @author Emanuel Rabina
 */
public interface Resource {

	/**
	 * Return the content of the resource after it has been processed.
	 * 
	 * @return Processed resource, or <tt>null</tt> if the resource has not yet
	 * 		   been processed.
	 */
	public String getProcessedContent();

	/**
	 * Return the content of the resource before it has been processed.
	 * 
	 * @return Original resource content.
	 */
	public String getSourceContent();

	/**
	 * Return whether or not the resource has been modified since the last time
	 * it was processed.
	 * 
	 * @return <tt>true</tt> if the resource has changed, <tt>false</tt>
	 * 		   otherwise.
	 */
	public boolean isModified();
}

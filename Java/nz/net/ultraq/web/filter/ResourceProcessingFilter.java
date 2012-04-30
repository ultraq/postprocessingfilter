package nz.net.ultraq.web.filter;



import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Parent class for any filter that does post-processing on file resources found
 * on a Java web application.  Enforces a common pattern for child filters, and
 * includes a resource cache so that unecessary processing can be avoided.
 * 
 * @author Emanuel Rabina
 */
public abstract class ResourceProcessingFilter implements Filter {

	private final HashMap<String,Resource> resourcecache = new HashMap<>();

	/**
	 * Allows a resource request to first go through to the application server,
	 * then checks to see if the resource has been processed or if the processed
	 * result has changed since the last time that resource was processed.  If
	 * it hasn't been processed or if it has changed, invokes the implementing
	 * filter to do its post-processing tasks and caches that result.  If it
	 * hasn't changed, it just uses the one already in the cache.
	 * 
	 * @param req
	 * @param res
	 * @param chain
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public final void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
		throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest)req;
		HttpServletResponse response = (HttpServletResponse)res;

		// Capture the resource file
		ResourceResponseWrapper resourceresponsewrapper = new ResourceResponseWrapper(response);
		chain.doFilter(request, resourceresponsewrapper);

		// Do nothing if file not modified
		if (resourceresponsewrapper.getStatus() == HttpServletResponse.SC_NOT_MODIFIED) {
			return;
		}

		// Use URL as the cache key
		StringBuffer urlbuilder = request.getRequestURL();
		if (request.getQueryString() != null) {
			urlbuilder.append(request.getQueryString());
		}
		String url = urlbuilder.toString();

		// Create a new processing result
		Resource resource;
		if (!resourcecache.containsKey(url) || resourcecache.get(url).isModified()) {
			resource = doProcessing(null, null);
			resourcecache.put(url, resource);
		}
		// Use the existing result in cache
		else {
			resource = resourcecache.get(url);
		}

		// Write processed result to response
		response.setContentLength(resource.getProcessedContent().getBytes().length);
		response.getOutputStream().write(resource.getProcessedContent().getBytes());
	}

	/**
	 * Perform post-processing on the given resource.
	 * 
	 * @param request
	 * @param resourcecontent String contents of the resource.
	 * @return A processed version of the resource.
	 */
	protected abstract Resource doProcessing(HttpServletRequest request, StringReader resourcecontent);
}

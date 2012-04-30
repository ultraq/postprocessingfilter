
package nz.net.ultraq.web.filter;

import java.io.ByteArrayOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Response wrapper to capture resources.
 * 
 * @author Emanuel Rabina
 */
public class ResourceResponseWrapper extends HttpServletResponseWrapper {

	private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
	private ServletOutputStream sos;

	/**
	 * Constructor, set the original response.
	 * 
	 * @param response
	 */
	public ResourceResponseWrapper(HttpServletResponse response) {

		super(response);
	}

	/**
	 * Return the resource bytes.
	 * 
	 * @return Output stream containing the resource.
	 */
	public ByteArrayOutputStream getResourceBytes() {

		return bos;
	}

	/**
	 * Return an output stream different from that of the wrapped response to
	 * capture the resource.
	 * 
	 * @return Output stream for the resource.
	 */
	@Override
	public ServletOutputStream getOutputStream() {

		if (sos == null) {
			sos = new ServletOutputStream() {
				@Override
				public void write(int b) {
					bos.write(b);
				}
			};
		}
		return sos;
	}
}

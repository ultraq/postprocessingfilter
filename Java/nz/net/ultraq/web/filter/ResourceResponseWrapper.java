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

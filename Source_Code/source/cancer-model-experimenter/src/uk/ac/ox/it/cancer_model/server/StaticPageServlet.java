/**
 * 
 */
package uk.ac.ox.it.cancer_model.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Responds to web page requests by getting the file from ARC
 * 
 * @author Ken Kahn
 *
 */
public class StaticPageServlet extends HttpServlet {

	private static final long serialVersionUID = 3075405196542994862L;
	private static final String HTML_CONTENT_TYPE = "text/html; charset=utf-8";
	private static final String PLAIN_CONTENT_TYPE = "text/plain; charset=utf-8";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, java.io.IOException {
		// following may try again (but only once due to use firstTime flag)
		respondToGet(request, response, true);
	}

	protected void respondToGet(HttpServletRequest request, HttpServletResponse response, boolean firstTime)
			throws IOException {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null) {
			return;
		}
		response.setCharacterEncoding("UTF-8");
		int dotIndex = pathInfo.lastIndexOf(".");
		String extension = pathInfo.substring(dotIndex + 1);
		int numberIndex = pathInfo.indexOf("batches-");
		ServletOutputStream outputStream = response.getOutputStream();
		SecureShell secureShell;
		try {
			secureShell = new SecureShell();
		} catch (Exception e) {
			outputStream.print("An error occurred contacting the ARC computers: " + e.getMessage());
			return;
		}
		if (extension.equalsIgnoreCase("html") || extension.equalsIgnoreCase("log")) {
			response.setContentType(HTML_CONTENT_TYPE);
		} else {
			response.setContentType(PLAIN_CONTENT_TYPE);
		}
		if (numberIndex > 0) {
//			outputStream.println("Contacting the server. Please wait.");
//			outputStream.flush();
			String numberOfBatches = pathInfo.substring(1, numberIndex);
			String uuid = pathInfo.substring(numberIndex + "batches-".length(), dotIndex);
			String remoteFileName = "/data/donc-onconet/share/cancer-outputs/" + uuid + "." + extension; 
			// old directory: /home/donc-onconet/oucs0030
			String command = "cd /data/donc-onconet/share/cancer/ && bash make_html.sh " + uuid + " " + numberOfBatches;
			secureShell.execute(command);
//			response.resetBuffer();
			if (!secureShell.copyRemoteFile(remoteFileName, outputStream) && extension.equals("log")) {
				// log file hasn't been created yet
				outputStream.print("Job is still in the ARC queue and yet to begin. Try again later.");
			}
		} else {
			// is some auxiliary file such as CSS or JS
			secureShell.copyRemoteFile("/data/donc-onconet/share/cancer-outputs" + pathInfo, outputStream);
		}
		secureShell.close();
	}

}

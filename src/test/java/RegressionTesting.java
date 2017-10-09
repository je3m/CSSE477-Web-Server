import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import server.Server;

public class RegressionTesting {
	Server server;

	public HttpURLConnection request(String type, String file, String[][] headers, String body) {
		HttpURLConnection connection = null;
		try {

			// Create connection
			URL url = new URL("http://localhost:8080/" + file);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(type);

			if (headers != null) {
				for (String[] s : headers) {
					connection.setRequestProperty(s[0], s[1]);
				}
			}

			connection.setDoOutput(true);

			if (body != null) {
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(body);
				wr.close();

			} else {
				connection.connect();
			}

			return connection;

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return null;
	}

	@Before
	public void setUp() throws InterruptedException {
		// TODO: Server configuration, ideally we want to read these from an
		// application.properties file
		String rootDirectory = "webtest";
		int port = 8080;

		// Create a run the server
		this.server = new Server(rootDirectory, port);
		Thread runner = new Thread(this.server);
		runner.start();

		// TODO: Instead of just printing to the console, use proper logging mechanism.
		// SL4J/Log4J are some popular logging framework
		System.out.format("Simple Web Server started at port %d and serving the %s directory ...%n", port,
				rootDirectory);
	}

	@After
	public void tearDown() throws IOException {
		this.server.stop();

		File postTxt = new File("webtest/post.txt");
		postTxt.delete();
		postTxt.createNewFile();
		FileWriter writer = new FileWriter(postTxt);
		writer.write("post that good stuff here:");
		writer.close();

		File rm = new File("webtest/post.fakenews");
		rm.delete();

		File del = new File("webtest/delete.txt");
		del.createNewFile();
		writer = new FileWriter(del);
		writer.write("delete me");
		writer.close();

		File put = new File("webtest/put.txt");
		put.createNewFile();
		writer = new FileWriter(put);
		writer.write("replace me");
		writer.close();

		rm = new File("webtest/put.fakenews");
		rm.delete();

	}

	@Test
	public void testGet() throws IOException {
		// good get
		HttpURLConnection connection = this.request("GET", "get.txt", null, null);
		InputStream is = connection.getInputStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
		String line;
		while ((line = rd.readLine()) != null) {
			response.append(line);
			response.append("\r\n");
		}
		rd.close();
		assertEquals(200, connection.getResponseCode());
		assertEquals("Got this file, fam\r\n", response.toString());

		// 404 get
		connection = this.request("GET", "get.fakenews", null, null);
		assertEquals(404, connection.getResponseCode());

	}

	@Test
	public void testPost() throws IOException {
		String stuffToPost = "posted stuff";

		// exist POST
		HttpURLConnection connection = this.request("POST", "post.txt", null, stuffToPost);
		InputStream is = connection.getInputStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		StringBuilder response = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			response.append(line);
			response.append("\r\n");
		}
		rd.close();
		assertEquals(200, connection.getResponseCode());
		assertEquals("post that good stuff here:" + stuffToPost + "\r\n", response.toString());

		// noexist post
		connection = this.request("POST", "post.fakenews", null, stuffToPost);
		is = connection.getInputStream();
		rd = new BufferedReader(new InputStreamReader(is));
		response = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			response.append(line);
			response.append("\r\n");
		}
		rd.close();
		assertEquals(200, connection.getResponseCode());
		assertEquals(stuffToPost + "\r\n", response.toString());
	}

	@Test
	public void testDelete() throws IOException {

		// get thing

		HttpURLConnection connection = this.request("GET", "delete.txt", null, null);
		InputStream is = connection.getInputStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
		String line;
		while ((line = rd.readLine()) != null) {
			response.append(line);
			response.append("\r\n");
		}
		rd.close();
		assertEquals(200, connection.getResponseCode());
		assertEquals("delete me\r\n", response.toString());

		// remove thing
		connection = this.request("DELETE", "delete.txt", null, null);
		assertEquals(200, connection.getResponseCode());

		// it's not there
		connection = this.request("GET", "delete.txt", null, null);
		assertEquals(404, connection.getResponseCode());

		// noexist DELETE
		connection = this.request("DELETE", "delete.fakenews", null, null);
		assertEquals(404, connection.getResponseCode());
	}

	@Test
	public void testPut() throws IOException {
		String stuffToPost = "put stuff";
		// exist POST
		HttpURLConnection connection = this.request("PUT", "put.txt", null, stuffToPost);
		InputStream is = connection.getInputStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		StringBuilder response = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			response.append(line);
			response.append("\r\n");
		}
		rd.close();
		assertEquals(200, connection.getResponseCode());
		assertEquals(stuffToPost + "\r\n", response.toString());

		// noexist post
		connection = this.request("PUT", "put.fakenews", null, stuffToPost);
		is = connection.getInputStream();
		rd = new BufferedReader(new InputStreamReader(is));
		response = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			response.append(line);
			response.append("\r\n");
		}
		rd.close();
		assertEquals(200, connection.getResponseCode());
		assertEquals(stuffToPost + "\r\n", response.toString());
	}

	@Test
	public void testHead() throws IOException {
		// good HEAD
		HttpURLConnection connection = this.request("HEAD", "get.txt", null, null);
		InputStream is = connection.getInputStream();

		assertEquals(200, connection.getResponseCode());
		assertEquals(-1, connection.getInputStream().read());

		// 404 head
		connection = this.request("HEAD", "get.fakenews", null, null);
		assertEquals(404, connection.getResponseCode());

	}
}
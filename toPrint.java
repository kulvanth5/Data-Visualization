package saireplica.department;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import jcifs.util.Base64;

import com.gargoylesoftware.htmlunit.FormEncodingType;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import com.opensymphony.xwork2.ActionSupport;

import resources.saireplicaConnMgr;
import saireplica.department.printOrder;
import saireplica.teacher.filePrint;;

public class toPrint extends ActionSupport {
	private String userid = null;
	private String orderid = null;
	private String type = null;
	private String totype = null;
	private InputStream inputStream;

	synchronized public String execute() {
		if (userid == null || orderid == null || type == null) {
			// System.out.println("Some thing is null");
			try {
				setInputStream(new ByteArrayInputStream("Null Error".getBytes("UTF-8")));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return SUCCESS;
		}
		boolean flag = false;
		if (type.equals("0")) {
			printOrder obj = new printOrder(orderid);
			flag = obj.createFile();
		} else if (type.equals("1")) {
			if (totype == null) {
				try {
					setInputStream(new ByteArrayInputStream("Teacher Order Type is NULL".getBytes("UTF-8")));
					// System.out.println("totype is null");
					return SUCCESS;
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			filePrint obj = new filePrint(orderid, totype);
			flag = obj.createFile();
			// System.out.println("File Created");
		}
		String filedir = null;
		if (!flag) {
			try {
				setInputStream(new ByteArrayInputStream("Null Error".getBytes("UTF-8")));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return SUCCESS;
		} else {
			try {
				Properties properties = new Properties();
				FileInputStream in = null;
				try {
					String resource = saireplicaConnMgr.class.getResource("AppRes.properties").toString();
					resource = resource.substring(5, resource.length());
					in = new FileInputStream(resource);
					properties.load(in);
					in.close();
				} catch (Exception e) {
					// System.out.println("Exception From updateMisc:"+e);
				}
				String encryptKey = properties.getProperty("saireplica.db.encKey");
				Connection con = (Connection) saireplicaConnMgr.getConnection();
				Statement stmt = (Statement) con.createStatement();
				int status = 0;
				String query = "select value from misc where id=10";
				ResultSet res = stmt.executeQuery(query);
				while (res.next()) {
					filedir = res.getString("value");
					if (type.equals("1"))
						filedir += orderid;// Because teacher may give print again. We keep the filename distinct.(Check
											// filePrint class for clarity)
				}
				query = "select status_id from user_order where order_id= '" + orderid + "' ;";
				res = stmt.executeQuery(query);
				while (res.next()) {
					status = res.getInt("status_id");
				}
				if (status == 3) {
					query = "update user_order set status_id= 4 where order_id= '" + orderid + "' ;";
					flag = stmt.execute(query);
					String filename = null;
					String CPN = null;
					String COLT = "ON";// Multiple Prints must complete set after set not ith page n(Quantity) times
					String DUP = null;
					String IT = "AUTO";// Paper Supply
					String SIZ = "A4";// Size of the Paper
					String MED = "NUL";// Paper Type
					String DEL = "IMP";// When to print (IMP= Immediate Print)
					ResultSet rs = null;
					int tpages = 0;
					int qty = 0;
					String uname = null;
					String pass = null;
					String purl = null;
					String srurl = null;
					query = "select des_decrypt(value,\"" + encryptKey + "\") as value from misc where id=2;";
					rs = stmt.executeQuery(query);
					while (rs.next()) {
						uname = rs.getString("value");
					}
					query = "select des_decrypt(value,\"" + encryptKey + "\") as value from misc where id=3;";
					rs = stmt.executeQuery(query);
					while (rs.next()) {
						pass = rs.getString("value");
					}
					query = "select des_decrypt(value,\"" + encryptKey + "\") as value from misc where id=5;";
					rs = stmt.executeQuery(query);
					while (rs.next()) {
						purl = rs.getString("value");
					}
					query = "select des_decrypt(value,\"" + encryptKey + "\") as value from misc where id=9;";
					rs = stmt.executeQuery(query);
					while (rs.next()) {
						srurl = rs.getString("value");
					}
					query = " select quantity,filename,start_pg,end_pg,quantity,2sideprint  from user_order where order_id='"
							+ orderid + "';";
					rs = stmt.executeQuery(query);
					while (rs.next()) {
						qty = rs.getInt("quantity");
						filename = rs.getString("filename");
						filedir = filedir.concat(filename);
						CPN = rs.getString("quantity");
						DUP = rs.getString("2sideprint");
						tpages = rs.getInt("end_pg") - rs.getInt("start_pg") + 1;
						if (DUP.equals("1")) {
							DUP = "0";
						} else if (DUP.equals("2")) {
							DUP = "3";
						} else if (DUP.equals("3")) {
							DUP = "4";
						}
					}
					query = "select value from misc where id= 4";
					rs = stmt.executeQuery(query);
					float price = 0;
					while (rs.next()) {
						price = Float.parseFloat(rs.getString("value"));
					}
					String authStr = uname + ":" + pass;
					String encoding = Base64.encode(authStr.getBytes());
					/* Sending file to the printer */
					URL url = new URL(srurl + "printer.html");
					// System.out.println(url);
					WebClient webClient = new WebClient();
					WebRequest req = new WebRequest(url);
					/* Setting values in settings page */
					// Source: https://chillyfacts.com/java-send-soap-xml-request-read-response/
					try {
						String SOAPurl = purl + "ssm/Management/SystemResource";
						URL obj = new URL(SOAPurl);
						HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
						conn.setRequestMethod("POST");
						conn.setRequestProperty("Authorization", "Basic " + encoding);
						conn.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
						String xml = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
								+ "<soap:Header xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
								+ "<msg:MessageInformation xmlns:msg=\"http://www.fujixerox.co.jp/2014/08/ssm/management/message\">"
								+ "<msg:MessageExchangeType>RequestResponse</msg:MessageExchangeType>"
								+ "<msg:MessageType>Request</msg:MessageType>"
								+ "<msg:Action>http://www.fujixerox.co.jp/2014/08/ssm/management/systemresource#SetSystemResource</msg:Action>"
								+ "<msg:From><msg:Address>http://www.fujixerox.co.jp/2014/08/ssm/management/soap/epr/client</msg:Address>"
								+ "<msg:ReferenceParameters/></msg:From></msg:MessageInformation>" + "</soap:Header>"
								+ "<soap:Body>"
								+ "<sr:SetSystemResource xmlns:sr=\"http://www.fujixerox.co.jp/2014/08/ssm/management/systemresource\">"
								+ "<sr:Resource><sr:Label>PdfJobMemNumCopy</sr:Label>"
								+ "<sr:Value>"
								+ "<sr:UnsignedIntValue>"
								+ CPN
								+ "</sr:UnsignedIntValue>"
								+ "</sr:Value>"
								+ "</sr:Resource>"
								+ "<sr:Resource><sr:Label>PdfJobMemDupMode</sr:Label>" + "<sr:Value>"
								+ "<sr:UnsignedIntValue>"
								+ DUP
								+ "</sr:UnsignedIntValue>" + "</sr:Value>" + "</sr:Resource>"
								+ "</sr:SetSystemResource>" + "</soap:Body>" + "</soap:Envelope>";
						conn.setDoOutput(true);
						DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
						wr.writeBytes(xml);
						wr.flush();
						wr.close();
						String responseStatus = conn.getResponseMessage();
						//System.out.println(responseStatus);
						BufferedReader SOAPin = new BufferedReader(new InputStreamReader(conn.getInputStream()));
						String inputLine;
						StringBuffer response = new StringBuffer();
						while ((inputLine = SOAPin.readLine()) != null) {
							response.append(inputLine);
						}
						SOAPin.close();
						//System.out.println("response:" + response.toString());
					} catch (Exception e) {
						//System.out.println(e);
					}
					/* End of setting values */
					HtmlPage printPage = webClient.getPage(req);

					HtmlForm form = printPage.getFormByName("F");
					HtmlFileInput file = (HtmlFileInput) form.getInputByName("FILE");
					file.setValueAttribute(filedir);
					file.setContentType("application/pdf");
					HtmlInput sub = form.getInputByName("sub");
					WebRequest reqSet = new WebRequest(new URL(purl + "UPLPRT.cmd"), HttpMethod.POST);
					//WebRequest reqSet = new WebRequest(new URL("http://192.168.34.153:8080/Sai_Replica/"+"printtrial"), HttpMethod.POST);
					List<NameValuePair> formF = form.getParameterListForSubmit(sub);
					reqSet.setRequestParameters(formF);
					reqSet.setEncodingType(FormEncodingType.MULTIPART);
					// reqSet.getRequestParameters().remove(16);
					// System.out.println("Data Sent:" + reqSet.getRequestParameters());
					webClient.addRequestHeader("Authorization", "Basic " + encoding);
					String htmlcode = webClient.getPage(reqSet).getWebResponse().getContentAsString();
					// System.out.println(htmlcode);
					webClient.close();
					if (htmlcode.equals("{\"result\":\"0\",\"errorCode\":\"0\"}")) {
						query = "update user_order set status_id= 5 where order_id= '" + orderid + "' ;";
						flag = stmt.execute(query);
						float cost = (float) (qty * tpages * price);
						query = "insert into completed_jobs values ('" + orderid + "',null,'" + tpages + "','" + cost
								+ "','" + userid + "');";
						flag = stmt.execute(query);
						if (type.equals("1")) {
							if (totype.equals("1")) {
								query = "update teacher_file set File='' where order_id=" + orderid + ";";
								flag = stmt.execute(query);
							} else if (totype.equals("2")) {
								query = "update teacher4student set File='' where order_id=" + orderid + ";";
								flag = stmt.execute(query);
							}
						} else {
							deleteFile(filedir);
						}
						setInputStream(new ByteArrayInputStream("Print Sent".getBytes("UTF-8")));
						return SUCCESS;
					}
				} else {
					setInputStream(new ByteArrayInputStream(
							"Somebody is already printing this document...!!!".getBytes("UTF-8")));
					deleteFile(filedir);
					return SUCCESS;
				}

			} catch (Exception ex) {
				try {
					Connection con = (Connection) saireplicaConnMgr.getConnection();
					Statement stmt = (Statement) con.createStatement();
					String query = "update user_order set status_id= 3 where order_id= '" + orderid + "' ;";
					stmt.execute(query);
				} catch (Exception e) {
					// System.out.println("Error from toprint.java");
					// System.out.println("Error while setting status back to 3");
					// System.out.println("Exception 1 from toPrint:"+e);
				}
				String error = ex.toString();
				// System.out.println("Exception 2 from toPrint:" + error);
				if (error.contains("503")) {
					try {
						setInputStream(new ByteArrayInputStream("503".getBytes("UTF-8")));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				} else if (error.contains("401")) {
					try {
						setInputStream(new ByteArrayInputStream("401".getBytes("UTF-8")));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				} else {
					try {
						setInputStream(new ByteArrayInputStream("400".getBytes("UTF-8")));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
		}

		deleteFile(filedir);
		return SUCCESS;
	}

	protected void deleteFile(String filedir) {
		File upFile = new File(filedir);
		if (upFile.exists()) {
			upFile.delete();
		}
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getOrderid() {
		return orderid;
	}

	public void setOrderid(String orderid) {
		this.orderid = orderid;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTotype() {
		return totype;
	}

	public void setTotype(String totype) {
		this.totype = totype;
	}
}

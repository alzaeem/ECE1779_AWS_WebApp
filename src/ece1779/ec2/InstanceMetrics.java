package ece1779.ec2;

import java.io.IOException;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.*;
import com.amazonaws.services.cloudwatch.model.*;

public class InstanceMetrics extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Instance Metrics</title>");
        out.println("</head>");
        out.println("<body>");
  
        //cloudWatchExample(out);
        getCPUUtilization(out);
        
        out.println("</body>");
        out.println("</html>");
    }
    
       
    void cloudWatchExample(PrintWriter out) throws IOException {
    	
    	
    	BasicAWSCredentials awsCredentials = (BasicAWSCredentials)getServletContext().getAttribute("AWSCredentials");
    	
		
    	
        AmazonCloudWatch cw = new AmazonCloudWatchClient(awsCredentials);
        
   
        try {

        	ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        	listMetricsRequest.setMetricName("CPUUtilization");
        	listMetricsRequest.setNamespace("AWS/EC2");
        	ListMetricsResult result = cw.listMetrics(listMetricsRequest);
        	java.util.List<Metric> 	metrics = result.getMetrics();
        	for (Metric metric : metrics) {
        		String namespace = metric.getNamespace();
        		String metricName = metric.getMetricName();
        		List<Dimension> dimensions = metric.getDimensions();
            	GetMetricStatisticsRequest statisticsRequest = new GetMetricStatisticsRequest();
            	statisticsRequest.setNamespace(namespace);
            	statisticsRequest.setMetricName(metricName);
            	statisticsRequest.setDimensions(dimensions);
            	Date endTime = new Date();
            	Date startTime = new Date();
            	startTime.setTime(endTime.getTime()-1200000);
            	statisticsRequest.setStartTime(startTime);
            	statisticsRequest.setEndTime(endTime);
            	statisticsRequest.setPeriod(60);
            	Vector<String>statistics = new Vector<String>();
            	statistics.add("Maximum");
            	statisticsRequest.setStatistics(statistics);
            	GetMetricStatisticsResult stats = cw.getMetricStatistics(statisticsRequest);
            	
            	out.print("<p>");
            	out.print("Namespace = " + namespace + " Metric = " + metricName + " Dimensions = " + dimensions);
            	out.print("Values = " + stats.toString());
            	out.println("</p>");
            	
        		
            }
        	
        } catch (AmazonServiceException ase) {
            out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon EC2, but was rejected with an error response for some reason.");
            out.println("Error Message:    " + ase.getMessage());
            out.println("HTTP Status Code: " + ase.getStatusCode());
            out.println("AWS Error Code:   " + ase.getErrorCode());
            out.println("Error Type:       " + ase.getErrorType());
            out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with EC2, "
                    + "such as not being able to access the network.");
            out.println("Error Message: " + ace.getMessage());
        }
    }
    
    void getCPUUtilization(PrintWriter out) throws IOException {
    	
    	BasicAWSCredentials awsCredentials = (BasicAWSCredentials)getServletContext().getAttribute("AWSCredentials");

        AmazonCloudWatch cw = new AmazonCloudWatchClient(awsCredentials);

    	/* print instance entries */
    	try {
    		/* print table header */
        	out.println("<table border='1'");
        	out.println("<tr bgcolor='silver'>");
        	out.println("<td> Instance ID</td>");
        	out.println("<td> CPU utilization</td>");
        	out.println("</tr>");
        	out.println("</table>");
    		
    		/* Filter for instances running our AMI */
    		List<DimensionFilter> amiFilter = new ArrayList<DimensionFilter>();
    		amiFilter.add(new DimensionFilter().withName("ImageId").withValue("ami-2d888444"));
    		
    		ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        	listMetricsRequest.setMetricName("CPUUtilization");
        	listMetricsRequest.setNamespace("AWS/EC2");
        	listMetricsRequest.setDimensions(amiFilter);

        	ListMetricsResult result = cw.listMetrics(listMetricsRequest);
        	java.util.List<Metric> 	metrics = result.getMetrics();

        	for (Metric metric : metrics) {
        		String namespace = metric.getNamespace();
        		String metricName = metric.getMetricName();
        		List<Dimension> dimensions = metric.getDimensions();
        		
            	GetMetricStatisticsRequest statisticsRequest = new GetMetricStatisticsRequest();
            	statisticsRequest.setNamespace(namespace);
            	statisticsRequest.setMetricName(metricName);
            	statisticsRequest.setDimensions(dimensions);

            	Date endTime = new Date();
            	Date startTime = new Date(0);
            	startTime.setTime(endTime.getTime()-1200000); /* 2 minutes */

            	statisticsRequest.setStartTime(startTime);
            	statisticsRequest.setEndTime(endTime);
            	statisticsRequest.setPeriod(60); /* 60 second granulatiry */
            	Vector<String>statistics = new Vector<String>();
            	statistics.add("Maximum");
            	statisticsRequest.setStatistics(statistics);
            	GetMetricStatisticsResult stats = cw.getMetricStatistics(statisticsRequest);
            	
            	/* get the latest CPU timestamp */
            	List<Datapoint> dataPoints = stats.getDatapoints();
            	Date latestTime = new Date(0);
            	Datapoint latestPoint = null;
            	for (Datapoint dataPoint : dataPoints) {
            		if (dataPoint.getTimestamp().after(latestTime)) {
            			latestTime = dataPoint.getTimestamp();
            			latestPoint = dataPoint;
            		}
            	}

            	out.println("<p> Namespace: " + namespace + " MetricName: " + metricName + " </p>");
            	out.print("<p> Dimensions: " + dimensions + " </p>");
            	out.print("<p> Values: " + stats.toString() + " </p>");
            	out.print("<p> Latest CPU: " + latestPoint.getMaximum() + " </p>");
            	out.println("<br>");
        	}

        } catch (AmazonServiceException ase) {
            out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon EC2, but was rejected with an error response for some reason.");
            out.println("Error Message:    " + ase.getMessage());
            out.println("HTTP Status Code: " + ase.getStatusCode());
            out.println("AWS Error Code:   " + ase.getErrorCode());
            out.println("Error Type:       " + ase.getErrorType());
            out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with EC2, "
                    + "such as not being able to access the network.");
            out.println("Error Message: " + ace.getMessage());
        }
    }
}
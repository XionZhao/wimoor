package com.wimoor.amazon.report.service.impl;

import com.amazon.spapi.client.ApiCallback;
import com.amazon.spapi.client.ApiException;
import com.amazon.spapi.model.reports.GetReportsResponse;
import com.amazon.spapi.model.reports.Report;
import com.amazon.spapi.model.reports.ReportList;
import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.report.service.IReportService;

import java.util.List;
import java.util.Map;

public class ApiCallbackGetReports implements ApiCallback<GetReportsResponse> {
	AmazonAuthority amazonAuthority=null;
	IReportService reportService=null;
	ApiCallbackGetReports(IReportService rpt,AmazonAuthority auth){
		 this.amazonAuthority=auth;
		 this.reportService=rpt;
	}
	@Override
	public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
		// TODO Auto-generated method stub
		System.out.println("[TransactionReport] getReportsAsync FAILED, statusCode=" + statusCode + ", seller=" + (amazonAuthority!=null ? amazonAuthority.getSellerid() : "null"));
		System.out.println("[TransactionReport] Error message: " + e.getMessage());
		System.out.println("[TransactionReport] Error response body: " + e.getResponseBody());
		System.out.println("[TransactionReport] Error headers: " + responseHeaders);
		e.printStackTrace();
	}

	@Override
	public void onSuccess(GetReportsResponse result, int statusCode, Map<String, List<String>> responseHeaders) {
		// TODO Auto-generated method stub
		System.out.println("[TransactionReport] getReportsAsync callback triggered, statusCode=" + statusCode);
		if(amazonAuthority!=null&&result!=null) {
			   ReportList list = result.getReports();
			   System.out.println("[TransactionReport] Found " + (list != null ? list.size() : 0) + " reports for seller: " + amazonAuthority.getSellerid());
			if(list!=null) {
				for(Report report:list) {
					  System.out.println("[TransactionReport] Processing reportId=" + report.getReportId() 
					      + ", status=" + report.getProcessingStatus()
					      + ", reportType=" + report.getReportType()
					      + ", documentId=" + report.getReportDocumentId());
					  reportService.recordReportRequest(amazonAuthority,report);
				}
		  }
	   } else {
		   System.out.println("[TransactionReport] Callback result is null, amazonAuthority=" + (amazonAuthority!=null));
	   }
	}

	@Override
	public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
		// TODO Auto-generated method stub

	}

}

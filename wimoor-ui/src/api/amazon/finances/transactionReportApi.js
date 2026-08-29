import request from "@/utils/request.js";

function getFeeSummaryByType(data) {
  return request.post("/amazon/api/v1/fin/transactionReport/getFeeSummaryByType", data);
}

function getDailyFeeByType(data) {
  return request.post("/amazon/api/v1/fin/transactionReport/getDailyFeeByType", data);
}

function getSalesCompare(data) {
  return request.post("/amazon/api/v1/fin/transactionReport/getSalesCompare", data);
}

function getDetailPage(data) {
  return request.post("/amazon/api/v1/fin/transactionReport/getDetailPage", data);
}

function exportDetail(params) {
  return request.post("/amazon/api/v1/fin/transactionReport/exportDetail", params, { responseType: 'blob' });
}

export default {
  getFeeSummaryByType,
  getDailyFeeByType,
  getSalesCompare,
  getDetailPage,
  exportDetail
};

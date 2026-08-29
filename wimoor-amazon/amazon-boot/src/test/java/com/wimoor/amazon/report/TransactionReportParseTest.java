package com.wimoor.amazon.report;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TransactionReport 解析测试
 * 验证 MarketplaceFacilitator 字段能正确识别
 */
public class TransactionReportParseTest {

    // 数值列特征关键词
    private static final String[] NUMERIC_PATTERNS = {
        "sales", "tax", "credits", "fees", "rebates", "rebat", "withheld", "total", "other"
    };

    public static void main(String[] args) {
        // 测试英文版
        System.out.println("=== 测试英文版 Transaction Report ===");
        String englishCsv = "\"date/time\",\"settlement id\",\"type\",\"order id\",\"sku\",\"description\",\"quantity\",\"marketplace\",\"account type\",\"fulfillment\",\"order city\",\"order state\",\"order postal\",\"tax collection model\",\"product sales\",\"product sales tax\",\"shipping credits\",\"shipping credits tax\",\"gift wrap credits\",\"giftwrap credits tax\",\"Regulatory Fee\",\"Tax On Regulatory Fee\",\"promotional rebates\",\"promotional rebates tax\",\"marketplace withheld tax\",\"selling fees\",\"fba fees\",\"other transaction fees\",\"other\",\"total\",\"Transaction Status\",\"Transaction Release Date\"\n" +
            "\"Jul 1, 2025 12:01:31 AM PDT\",\"23868213761\",\"Order\",\"112-1122518-5803405\",\"A5074FPYDB-APN\",\"Test Product\",1,\"amazon.com\",\"Standard Orders\",\"Amazon\",\"DEERFIELD\",\"IL\",\"60015\",\"MarketplaceFacilitator\",5.88,0,0,0,0,0,0,0,0,0,0,-0.88,-2.91,0,0,2.09,\"Released\",\"Jul 1, 2025 12:01:31 AM PDT\"\n" +
            "\"Jul 1, 2025 12:06:01 AM PDT\",\"23868213761\",\"Order\",\"114-6608027-2716216\",\"A3456FPFZB-APN\",\"Another Product\",1,\"amazon.com\",\"Standard Orders\",\"Amazon\",\"MEMPHIS\",\"TN\",\"38118\",\"MarketplaceFacilitator\",15.89,1.55,0,0,0,0,0,0,0,0,-1.55,-1.59,-5.03,0,0,9.27,\"Released\",\"Jul 1, 2025 12:06:01 AM PDT\"";
        
        testParseCsv(englishCsv);
        
        // 测试德语版
        System.out.println("\n=== 测试德语版 Transaction Report ===");
        String germanCsv = "\"Datum/Uhrzeit\",\"Abrechnungs-ID\",\"Typ\",\"Bestellnummer\",\"SKU\",\"Beschreibung\",\"Menge\",\"Verkaufsplattform\",\"Kontotyp\",\"Versandart\",\"Bestellstadt\",\"Bestellbundesland\",\"Bestellpostleitzahl\",\"Steuererhebungsmodell\",\"Produktverkäufe\",\"Produktverkäufe Steuer\",\"Versandgutschriften\",\"Versandgutschriften Steuer\",\"Geschenkverpackungsgutschriften\",\"Geschenkverpackungsgutschriften Steuer\",\"Regulatory Fee\",\"Tax On Regulatory Fee\",\"Werbevergütungen\",\"Werbevergütungen Steuer\",\"Einbehaltene Marktplatzsteuer\",\"Verkaufsgebühren\",\"FBA-Gebühren\",\"Sonstige Transaktionsgebühren\",\"Sonstiges\",\"Gesamt\",\"Transaktionsstatus\",\"Transaktionsfreigabedatum\"\n" +
            "\"1. Juli 2025 00:01:31 MESZ\",\"98765432101\",\"Bestellung\",\"112-1122518-5803405\",\"A5074FPYDB-APN\",\"Testprodukt\",1,\"amazon.de\",\"Standardbestellungen\",\"Amazon\",\"BERLIN\",\"BE\",\"10115\",\"MarketplaceFacilitator\",15.99,3.04,0,0,0,0,0,0,0,0,0,-2.40,-4.50,0,0,12.13,\"Freigegeben\",\"1. Juli 2025 00:01:31 MESZ\"";
        
        testParseCsv(germanCsv);
    }
    
    private static void testParseCsv(String csvContent) {
        BufferedReader br = new BufferedReader(new StringReader(csvContent));
        String line;
        boolean headerFound = false;
        Map<String, Integer> columnIndexMap = new HashMap<>();
        int lineNumber = 0;
        
        try {
            while ((line = br.readLine()) != null) {
                lineNumber++;
                
                if (!headerFound) {
                    String[] checkInfo = line.split(",");
                    if (checkInfo.length >= 20) {
                        System.out.println("找到列名在第" + lineNumber + "行，列数: " + checkInfo.length);
                        headerFound = true;
                        
                        List<String> headerFields = parseCsvLine(line);
                        for (int i = 0; i < headerFields.size(); i++) {
                            String colName = headerFields.get(i).trim().toLowerCase()
                                .replace("\"", "").replace("'", "");
                            columnIndexMap.put(colName, i);
                        }
                        System.out.println("列名映射: " + columnIndexMap);
                    }
                    continue;
                }
                
                List<String> fields = parseCsvLine(line);
                System.out.println("\n数据行" + lineNumber + " 字段数: " + fields.size());
                
                // 解析字段
                Map<String, String> parsedFields = new HashMap<>();
                for (Map.Entry<String, Integer> entry : columnIndexMap.entrySet()) {
                    String colName = entry.getKey();
                    int index = entry.getValue();
                    if (index < fields.size()) {
                        String value = fields.get(index).trim();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        parsedFields.put(colName, value);
                    }
                }
                
                // 识别字段类型
                System.out.println("tax collection model = " + parsedFields.get("tax collection model"));
                System.out.println("product sales = " + parsedFields.get("product sales"));
                System.out.println("selling fees = " + parsedFields.get("selling fees"));
                System.out.println("total = " + parsedFields.get("total"));
                
                // 验证数值列识别
                System.out.println("\n数值列识别:");
                for (Map.Entry<String, Integer> entry : columnIndexMap.entrySet()) {
                    String colName = entry.getKey();
                    if (isNumericColumn(colName)) {
                        System.out.println("  " + colName + " -> 数值列");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static List<String> parseCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        
        return fields;
    }
    
    private static boolean isNumericColumn(String colName) {
        if (colName == null) return false;
        String lower = colName.trim().toLowerCase();
        for (String pattern : NUMERIC_PATTERNS) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}

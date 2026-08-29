package com.wimoor.amazon.inboundV2.service;

import com.wimoor.amazon.inboundV2.XmlPojo.CustomsDeclaration;
import com.wimoor.amazon.inboundV2.XmlPojo.InventoryData;
import com.wimoor.amazon.inboundV2.XmlPojo.OrderData;
import com.wimoor.amazon.inboundV2.pojo.entity.ShipInboundCustomsXml;
import com.wimoor.common.user.UserInfo;

public interface IShipCrossborderXmlSevice {

    /**
     * 检查是否存在冲突记录（只读操作，不开启事务）
     * 当force=false时，如果存在冲突则抛出BizException
     */
    void checkConflict(ShipInboundCustomsXml xmlInfo);

    public ShipInboundCustomsXml createOrder(UserInfo userinfo, ShipInboundCustomsXml xmlInfo) throws Exception;
    public ShipInboundCustomsXml createInventory(UserInfo userinfo, ShipInboundCustomsXml xmlInfo) throws Exception;
    public ShipInboundCustomsXml createDeclaration(UserInfo userinfo, ShipInboundCustomsXml xmlInfo) throws Exception;
    /**
     * 示例2：生成电子订单
     */
    String generateOrder(UserInfo userinfo, ShipInboundCustomsXml xml) throws Exception ;
    String generateOrder(UserInfo userinfo, ShipInboundCustomsXml xml, OrderData orderData) throws Exception;

    String generateDeclaration(UserInfo userinfo, ShipInboundCustomsXml xml) throws Exception;
    String generateDeclaration(UserInfo userinfo, ShipInboundCustomsXml xml, CustomsDeclaration declaration) throws Exception;

    String generateInventory(UserInfo userinfo, ShipInboundCustomsXml xml) throws Exception;
    String generateInventory(UserInfo userinfo, ShipInboundCustomsXml xml, InventoryData inventoryData) throws Exception;

    String getFileName(ShipInboundCustomsXml xmlInfo);
     void markShipment(ShipInboundCustomsXml xmlInfo);
    String downloadShipXmlFile(UserInfo userinfo, ShipInboundCustomsXml xml) throws Exception;

}

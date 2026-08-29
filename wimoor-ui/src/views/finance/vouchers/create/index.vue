<template >

  <div class="app-container"  @click="clearEdit(form.entries);" >
    <div :style="widthStyle"  >
      <div class="action-bar" >
        <el-space>
          <el-button type="primary" @click="saveAndNew" :disabled="data.periodClosed" :title="data.periodClosed ? '已结账，不可操作' : ''">保存并新增</el-button>
          <el-button type="primary" @click="submitForm" :disabled="data.periodClosed" :title="data.periodClosed ? '已结账，不可操作' : ''">保存</el-button>
          <el-button @click="saveDraft" :disabled="data.periodClosed" :title="data.periodClosed ? '已结账，不可操作' : ''">暂存</el-button>
          <el-button @click="handleImportVoucherShow()">导入凭证</el-button>
          <el-dropdown  @command="handleDropdownCommand">
    <span class="el-dropdown-link">
       <el-button  >
        模板<el-icon class="el-icon--right"><arrow-down /></el-icon>
      </el-button>

    </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="saveAsTemplate">保存为凭证模版</el-dropdown-item>
                <el-dropdown-item command="createFromTemplate">从模版生成凭证</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!--      <el-dropdown @command="handlePreferences">
                  <el-button>偏好设置</el-button>
                </el-dropdown>-->
        </el-space>
        <div class="action-right">
          <PrinterBtn printId="voucherPrintArea" :link="true" title="凭证打印" />
          <el-button type="text" @click="showKeyboardShortcuts">快捷键</el-button>
          <el-button type="text" @click="toggleFullScreen">大屏</el-button>
          <el-button type="text" @click="prevVoucher" :disabled="queryParams.pageNum+1>total&&queryParams.pageNum!=0">‹</el-button>
          <el-button type="text" @click="nextVoucher" :disabled="queryParams.pageNum<1">›</el-button>
        </div>
      </div>


      <el-form ref="vouchersRef"  id="voucherPrintArea" :model="form" :rules="rules" label-width="0" >
        <div class="voucher-body">
          <div class="voucher-header" >
            <div class="voucher-main-info" style="position:relative;">
              <img v-if="data.periodClosed" :src="closedStampImg" class="seal-watermark" alt="已结账" />
              <div class="voucher-type-info">
                <span class="voucher-type-label" >凭证字</span>
                <el-select v-model="form.voucherType"
                           placeholder="记"
                           :disabled="data.periodClosed"
                           style="width:80px;margin-right: 5px; border-radius: 0; height: 30px;">
                  <el-option v-for="item in voucherTypeList" :key="item.id" :label="item.name" :value="item.name"></el-option>
                </el-select>
                <span class="voucher-no-label">号</span>
                <el-input v-model="form.voucherNo" placeholder="001" :disabled="data.periodClosed" style="width: 50px; text-align: center; height: 30px; border-radius: 0; margin-right: 30px;" />
                <div class="voucher-date-info">
                  <span class="voucher-date-label">日期</span>
                  <el-date-picker
                      v-model="form.voucherDate"
                      type="date"
                      @change="handleNextVoucherNo"
                      :disabled="data.periodClosed"
                      value-format="YYYY-MM-DD"
                      style="width: 130px; height: 30px; border-radius: 0;"
                  >
                  </el-date-picker>
                </div>
              </div>
              <div class="voucher-period-info" style="margin-right: 150px;">
                <h2 v-if="form.voucherType==='记'">记账凭证</h2>
                <h2 v-else-if="form.voucherType==='收'">收款凭证</h2>
                <h2 v-else-if="form.voucherType==='付'">付款凭证</h2>
                <h2 v-else-if="form.voucherType==='转'">转账凭证</h2>
                <h2 v-else>{{ form.voucherType }}凭证</h2>
                <div class="voucher-period">{{ new Date(form.voucherDate).getFullYear() }}年第{{ (new Date(form.voucherDate).getMonth() + 1)  }}期</div>
              </div>
              <div class="voucher-attachment-info">
                <el-button type="text" @click="searchVoucher" size="small">
                  <el-icon><Search /></el-icon>
                </el-button>
                <span>附件{{form.files?form.files.length:0}}张</span>
                <el-button type="primary" link @click="uploadAttachment" :disabled="data.periodClosed" size="small">
                  <el-icon><Upload /></el-icon>上传附件
                </el-button>
              </div>
            </div>
          </div>

          <div class="wimoor-voucher-table-wrapper">
            <table class="wimoor-voucher-table">
              <thead>
              <tr>
                <th class="row-number-col">序号</th>
                <th class="summary-col">摘要</th>
                <th class="account-col">科目</th>
                <th class="currency-col"  style="min-width:120px;width:10%" v-if="hasQuantity">数量</th>
                <th class="currency-col" style="min-width:180px;width:15%" v-if="hasCurrency">币别</th>
                <th>
                  <div class="amountHeaderWrapper--3TtBf"><div class="amountTitle--146pZ">借方金额</div><div class="amountUnit--XSR71"><div class="amountLineBgWrapper--1MZC6" style="font-size: 12px; font-weight: 400;"><div class="amountLine--2qaEF">亿</div><div class="amountLine--2qaEF">千</div><div class="amountLine--2qaEF">百</div><div class="amountLine--2qaEF">十</div><div class="amountLine--2qaEF">万</div><div class="amountLine--2qaEF">千</div><div class="amountLine--2qaEF">百</div><div class="amountLine--2qaEF">十</div><div class="amountLine--2qaEF">元</div><div class="amountLine--2qaEF">角</div><div class="amountLine--2qaEF">分</div></div></div></div>
                </th>
                <th>
                  <div class="amountHeaderWrapper--3TtBf"><div class="amountTitle--146pZ">贷方金额</div><div class="amountUnit--XSR71"><div class="amountLineBgWrapper--1MZC6" style="font-size: 12px; font-weight: 400;"><div class="amountLine--2qaEF">亿</div><div class="amountLine--2qaEF">千</div><div class="amountLine--2qaEF">百</div><div class="amountLine--2qaEF">十</div><div class="amountLine--2qaEF">万</div><div class="amountLine--2qaEF">千</div><div class="amountLine--2qaEF">百</div><div class="amountLine--2qaEF">十</div><div class="amountLine--2qaEF">元</div><div class="amountLine--2qaEF">角</div><div class="amountLine--2qaEF">分</div></div></div></div>
                </th>
              </tr>
              </thead>
              <tbody>
              <tr :id="index" v-for="(item, index) in form.entries" :key="index"  @click.stop="data.periodClosed ? null : (clearEdit(form.entries),item.rowEdit=true)"     class="voucher-row">
                <td class="row-number" @click.stop="data.periodClosed ? null : (clearEdit(form.entries),item.rowEdit=true)">
                  <el-space class="row-actions">
                    <el-button  link size="small" @click="insertRow(index)" icon="Plus" :disabled="data.periodClosed" title="插入行"></el-button>
                    <el-button link size="small" @click="copyRow(index)" icon="CopyDocument" :disabled="data.periodClosed" title="复制行"></el-button>
                    <el-button link size="small" @click="removeItem(index)" icon="Delete" :disabled="form.entries.length <= 1 || data.periodClosed" title="删除行"></el-button>
                  </el-space>

                  <div class="row-number-text">
                    {{ index + 1 }}
                  </div>

                </td>
                <td class="summary-col" @click.stop="data.periodClosed ? null : handleSummaryClick(index)">
                  <div style="width: 100%;">
                    <div class="flex-between" v-show="item.rowEdit">
                      <el-input
                          v-model="item.summary"
                          :ref="el => summaryInputs[index] = el"
                          :disabled="data.periodClosed"
                          @keyup.enter="focusNextCell($event, 'account', index)"
                          style="border: none; box-shadow: none; padding: 0; height: 60px;width:100%; font-size: 13px;"
                          class="wimoor-input">
                      </el-input>
                      <div style="padding-top:20px;">
                        <el-button
                            circle
                            size="small"
                            style="width:25px;"
                            class="row-actions"
                            :disabled="data.periodClosed"
                            @click="showSummarySuggestions(index)"
                            title="摘要选择"
                        >
                          <el-icon   ><MoreFilled /></el-icon>
                        </el-button>
                      </div>
                    </div>
                    <div v-show="!item.rowEdit" style="padding-left: 10px">{{item.summary}}</div>
                  </div>
                </td>
                <td class="account-col" @click.stop="data.periodClosed ? null : handleAccountClick(index)">
                  <el-select
                      v-show="item.rowEdit"
                      v-model="item.subjectId"
                      placeholder=""
                      filterable
                      automatic-dropdown
                      :disabled="data.periodClosed"
                      :ref="el => accountSelects[index] = el"
                      @change="handleAccountChange(item)"
                      @click.stop="item.rowEdit=true"
                      @keyup.enter="focusNextCell($event, 'debit', index)"
                      style="width: 100%;box-shadow: none; border: none; padding: 0; height: 60px; font-size: 13px;"
                  >
                    <el-option
                        v-for="account in accountList"
                        :key="account.subjectCode"
                        :label="`${account.subjectCode} ${account.subjectName}`"
                        :value="account.subjectId"
                    >
                      <div class="account-option">
                        <span>{{ account.subjectCode }}</span>
                        <span>{{ account.subjectName }}</span>
                        <span v-if="account.balance !== undefined" class="account-balance">余额: {{ formatCurrency(account.balance) }}</span>
                      </div>
                    </el-option>
                  </el-select>
                  <div v-show="!item.rowEdit"  style="padding-left:10px">
                    <span>{{ item.subjectCode }}</span>
                    {{item.subjectName}}
                    <span v-if="item.auxiliaryText" style="color:#999;font-size:11px;"> - {{ item.auxiliaryText }}</span>
                    <el-button
                        v-if="item.isAuxiliary"
                        circle
                        size="small"
                        style="width:25px;margin-left:4px;"
                        :disabled="data.periodClosed"
                        @click.stop="openAuxiliaryDialog(item)"
                        title="辅助核算"
                    >
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                  </div>
                </td>
                <td  class="quantity-col" v-if="hasQuantity"  @click.stop="data.periodClosed ? null : handleCurrencyClick(index, item,'quantity')">
                  <div v-show="item.rowEdit &&item.isQuantity" style="padding-left:2px;font-size:12px">
                  <div  class="flex">数量：<el-input
                      type="number" :disabled="!item.isQuantity || data.periodClosed"
                      :ref="el => quantityInputs[index] = el"
                      @input="handleInputChange(item)" style="width:60px;" size="small" v-model.number="item.quantity">  </el-input>个</div>
                  <div style="padding-top:2px;">单价：
                             <el-input type="number"
                             :disabled="!item.isQuantity || data.periodClosed"
                                       @click.stop="item.rowEdit=true"
                             @input="handleInputChange(item)"
                             style="width:70px;" size="small"
                             v-model.number="item.unitPrice"></el-input></div>
                  </div>
                  <div  v-show="!item.rowEdit && item.subjectId && item.isQuantity">
                    <div style="padding-left:2px;font-size:12px" class="flex">数量：{{item.quantity}}个</div>
                    <div style="padding-top:2px;padding-left:2px;font-size:12px">单价： {{item.unitPrice}}</div>
                  </div>
                </td>
                <td class="currency-col" v-if="hasCurrency" @click.stop="data.periodClosed ? null : handleCurrencyClick(index, item,'currency')">
                  <div v-show="item.rowEdit&&item.isForeignCurrency" style="padding-left:2px;font-size:12px">
                    <div class="flex">
                    <span style="font-size:12px;padding-left:2px">原币：</span><el-select
                      v-model="item.currency"
                      style="width:60px"
                      placeholder="币种"
                      @change="changeCurrency(item)"
                      @click.stop="item.rowEdit=true"
                      :disabled="!item.isForeignCurrency || data.periodClosed"
                      size="small"
                  >
                    <el-option
                        v-for="currency in currencyList"
                        :key="currency.code"
                        :label="currency.code"
                        :value="currency.code"
                    >
                      <div class="currency-option">
                        <span>{{ currency.code }}</span>
                        <span>{{ currency.name }}</span>
                      </div>
                    </el-option>
                  </el-select>
                  <el-input      @click.stop="item.rowEdit=true"     :disabled="!item.isForeignCurrency || data.periodClosed" type="number" style="margin-left:3px;width:70px" placeholder="金额" @input="handleInputChange(item)"  size="small" v-model.number="item.originalAmount"  ></el-input>
                </div>
                 <div style="padding-top:2px;padding-left:2px">汇率：
                   <el-input type="number"
                             :ref="el => exchangeRateInputs[index] = el"
                             :disabled="!item.isForeignCurrency || data.periodClosed"
                             @input="handleInputChange(item)"
                             @click.stop="item.rowEdit=true"
                             style="width:132px;margin-left:-3px;" size="small"
                             v-model.number="item.exchangeRate">

                   </el-input></div>
                </div>
              <div v-show="!item.rowEdit && item.subjectId &&item.isForeignCurrency" style="padding-left:2px;font-size:12px">
                  <div class="flex">
                   <span style="padding-left:2px;padding-right:5px">原币： {{item.currency}}</span>
                    金额： {{item.originalAmount}}
                  </div>
                 <div style="padding-top:2px;padding-left:2px">汇率：  {{item.exchangeRate}}</div>
              </div>
                </td>
                <td class="debit-col"  @click.stop="e => { if (!data.periodClosed) handleEditTrue(e,item,'debitEdit',index) }">
                  <div class="amount-input-wrapper" v-if="item.debitEdit">
                    <el-input
                        clearable
                        v-model.number="item.debitAmount"
                        type="number"
                        :disabled="data.periodClosed"
                        @blur="item.debitEdit=false"
                        @click.stop="item.debitEdit=true"
                        @input="calculateTotal(item)"
                        @clear="calculateTotal(item)"
                        @keyup.enter="focusNextCell($event, 'credit', index)"
                        @keyup.tab="focusNextCell($event, 'credit', index)"
                        style="border: none; box-shadow: none; padding: 0; text-align: right; height: 100%; font-size: 13px;"
                        class="wimoor-input"
                    />
                  </div>
                  <div v-else class="amountHeaderWrapper" >
                    <div class="amountUnit--XSR71" :style="{ color: item.debitAmount < 0 ? 'red' : '' }">
                      <div v-if="item.debitAmountChinese&&item.debitAmountChinese.length>0" class="amountLineBgWrapper--1MZC6" style="font-size: 12px;height:60px; font-weight: 400;">
                        <div class="amountLine--2qaEF" title="亿" >{{item.debitAmountChinese[0]}}</div>
                        <div class="amountLine--2qaEF" title="千" >{{item.debitAmountChinese[1]}}</div>
                        <div class="amountLine--2qaEF" title="百" >{{item.debitAmountChinese[2]}}</div>
                        <div class="amountLine--2qaEF" title="十" >{{item.debitAmountChinese[3]}}</div>
                        <div class="amountLine--2qaEF" title="万" >{{item.debitAmountChinese[4]}}</div>
                        <div class="amountLine--2qaEF" title="千" >{{item.debitAmountChinese[5]}}</div>
                        <div class="amountLine--2qaEF" title="百" >{{item.debitAmountChinese[6]}}</div>
                        <div class="amountLine--2qaEF" title="十" >{{item.debitAmountChinese[7]}}</div>
                        <div class="amountLine--2qaEF" title="元" >{{item.debitAmountChinese[8]}}</div>
                        <div class="amountLine--2qaEF" title="角" >{{item.debitAmountChinese[9]}}</div>
                        <div class="amountLine--2qaEF" title="分" >{{item.debitAmountChinese[10]}}</div>
                      </div>
                      <div v-else class="amountLineBgWrapper--1MZC6" style="font-size: 12px;height:60px; font-weight: 400;">
                        <div class="amountLine--2qaEF" title="亿" ></div>
                        <div class="amountLine--2qaEF" title="千" ></div>
                        <div class="amountLine--2qaEF" title="百" ></div>
                        <div class="amountLine--2qaEF" title="十" ></div>
                        <div class="amountLine--2qaEF" title="万" ></div>
                        <div class="amountLine--2qaEF" title="千" ></div>
                        <div class="amountLine--2qaEF" title="百" ></div>
                        <div class="amountLine--2qaEF" title="十" ></div>
                        <div class="amountLine--2qaEF" title="元" ></div>
                        <div class="amountLine--2qaEF" title="角" ></div>
                        <div class="amountLine--2qaEF" title="分" ></div>
                      </div>
                    </div>
                  </div>
                </td>
                <td class="credit-col" @click.stop="e => { if (!data.periodClosed) handleEditTrue(e,item,'creditEdit',index) }">
                  <div class="amount-input-wrapper" v-if="item.creditEdit">
                    <el-input
                        v-model.number="item.creditAmount"
                        @blur="item.creditEdit=false"
                        clearable
                        type="number"
                        :disabled="data.periodClosed"
                        @click.stop="item.creditEdit=true"
                        @clear="calculateTotal(item)"
                        @input="calculateTotal(item)"
                        @keyup.enter="focusNextRow(index)"
                        style="border: none; box-shadow: none; padding: 0; text-align: right; height: 100%; font-size: 13px;"
                        class="wimoor-input"
                    />
                  </div>
                  <div v-else class="amountHeaderWrapper" :style="{ color: item.creditAmount < 0 ? 'red' : '' }">
                    <div class="amountUnit--XSR71">
                      <div v-if="item.creditAmountChinese&&item.creditAmountChinese.length>10" class="amountLineBgWrapper--1MZC6" style="font-size: 12px;height:60px; font-weight: 400;">
                        <div class="amountLine--2qaEF" title="亿" >{{item.creditAmountChinese[0]}}</div>
                        <div class="amountLine--2qaEF" title="千" >{{item.creditAmountChinese[1]}}</div>
                        <div class="amountLine--2qaEF" title="百" >{{item.creditAmountChinese[2]}}</div>
                        <div class="amountLine--2qaEF" title="十" >{{item.creditAmountChinese[3]}}</div>
                        <div class="amountLine--2qaEF" title="万" >{{item.creditAmountChinese[4]}}</div>
                        <div class="amountLine--2qaEF" title="千" >{{item.creditAmountChinese[5]}}</div>
                        <div class="amountLine--2qaEF" title="百" >{{item.creditAmountChinese[6]}}</div>
                        <div class="amountLine--2qaEF" title="十" >{{item.creditAmountChinese[7]}}</div>
                        <div class="amountLine--2qaEF" title="元" >{{item.creditAmountChinese[8]}}</div>
                        <div class="amountLine--2qaEF" title="角" >{{item.creditAmountChinese[9]}}</div>
                        <div class="amountLine--2qaEF" title="分" >{{item.creditAmountChinese[10]}}</div>
                      </div>
                      <div  v-else class="amountLineBgWrapper--1MZC6" style="font-size: 12px;height:60px; font-weight: 400;">
                        <div class="amountLine--2qaEF" title="亿" ></div>
                        <div class="amountLine--2qaEF" title="千" ></div>
                        <div class="amountLine--2qaEF" title="百" ></div>
                        <div class="amountLine--2qaEF" title="十" ></div>
                        <div class="amountLine--2qaEF" title="万" ></div>
                        <div class="amountLine--2qaEF" title="千" ></div>
                        <div class="amountLine--2qaEF" title="百" ></div>
                        <div class="amountLine--2qaEF" title="十" ></div>
                        <div class="amountLine--2qaEF" title="元" ></div>
                        <div class="amountLine--2qaEF" title="角" ></div>
                        <div class="amountLine--2qaEF" title="分" ></div>
                      </div>
                    </div>
                  </div>
                </td>
              </tr>
              </tbody>
              <tfoot>
              <tr class="total-row">
                <td colspan="2">
                  <el-button
                      type="primary"
                      size="small"
                      @click="autoBalance"
                      :disabled="data.periodClosed"
                      v-if="!isBalance"
                      style="border-radius: 0; height: 28px; font-size: 12px; margin-left: 5px;"
                  >    <svg-icon icon-class="calculator-white"  style="color:#fff;width:14px;height:14px" />自动平衡</el-button>
                </td>
                <td v-if="hasQuantity"></td>
                <td v-if="hasCurrency"></td>
                <td class="total-label">合计</td>
                <td class="amountHeaderWrapper" :style="{ color: form.debitTotal < 0 ? 'red' : '' }">
                  <div class="amountUnit--XSR71">
                    <div v-if="form.debitTotalChinese&&form.debitTotalChinese.length>10" class="amountLineBgWrapper--1MZC6" style="font-size: 12px;height:60px; font-weight: 600;">
                      <div class="amountLine--2qaEF" title="亿" >{{form.debitTotalChinese[0]}}</div>
                      <div class="amountLine--2qaEF" title="千" >{{form.debitTotalChinese[1]}}</div>
                      <div class="amountLine--2qaEF" title="百" >{{form.debitTotalChinese[2]}}</div>
                      <div class="amountLine--2qaEF" title="十" >{{form.debitTotalChinese[3]}}</div>
                      <div class="amountLine--2qaEF" title="万" >{{form.debitTotalChinese[4]}}</div>
                      <div class="amountLine--2qaEF" title="千" >{{form.debitTotalChinese[5]}}</div>
                      <div class="amountLine--2qaEF" title="百" >{{form.debitTotalChinese[6]}}</div>
                      <div class="amountLine--2qaEF" title="十" >{{form.debitTotalChinese[7]}}</div>
                      <div class="amountLine--2qaEF" title="元" >{{form.debitTotalChinese[8]}}</div>
                      <div class="amountLine--2qaEF" title="角" >{{form.debitTotalChinese[9]}}</div>
                      <div class="amountLine--2qaEF" title="分" >{{form.debitTotalChinese[10]}}</div>
                    </div>
                    <div v-else class="amountLineBgWrapper--1MZC6" style="font-size: 12px;height:60px; font-weight: 600;">
                      <div class="amountLine--2qaEF" title="亿" ></div>
                      <div class="amountLine--2qaEF" title="千" ></div>
                      <div class="amountLine--2qaEF" title="百" ></div>
                      <div class="amountLine--2qaEF" title="十" ></div>
                      <div class="amountLine--2qaEF" title="万" ></div>
                      <div class="amountLine--2qaEF" title="千" ></div>
                      <div class="amountLine--2qaEF" title="百" ></div>
                      <div class="amountLine--2qaEF" title="十" ></div>
                      <div class="amountLine--2qaEF" title="元" ></div>
                      <div class="amountLine--2qaEF" title="角" ></div>
                      <div class="amountLine--2qaEF" title="分" ></div>

                    </div>
                  </div>
                </td>
                <td class="amountHeaderWrapper" :style="{ color: form.creditTotal < 0 ? 'red' : '' }">
                  <div class="amountUnit--XSR71">
                    <div  v-if="form.creditTotalChinese&&form.debitTotalChinese.length>10" class="amountLineBgWrapper--1MZC6" style="font-size: 12px;height:60px; font-weight: 600;">
                      <div class="amountLine--2qaEF" title="亿">{{form.creditTotalChinese[0]}}</div>
                      <div class="amountLine--2qaEF" title="千">{{form.creditTotalChinese[1]}}</div>
                      <div class="amountLine--2qaEF" title="百">{{form.creditTotalChinese[2]}}</div>
                      <div class="amountLine--2qaEF" title="十">{{form.creditTotalChinese[3]}}</div>
                      <div class="amountLine--2qaEF" title="万">{{form.creditTotalChinese[4]}}</div>
                      <div class="amountLine--2qaEF" title="千">{{form.creditTotalChinese[5]}}</div>
                      <div class="amountLine--2qaEF" title="百">{{form.creditTotalChinese[6]}}</div>
                      <div class="amountLine--2qaEF" title="十">{{form.creditTotalChinese[7]}}</div>
                      <div class="amountLine--2qaEF" title="元">{{form.creditTotalChinese[8]}}</div>
                      <div class="amountLine--2qaEF" title="角">{{form.creditTotalChinese[9]}}</div>
                      <div class="amountLine--2qaEF" title="分">{{form.creditTotalChinese[10]}}</div>
                    </div>
                    <div  v-else class="amountLineBgWrapper--1MZC6" style="font-size: 12px;height:60px; font-weight: 600;">
                      <div class="amountLine--2qaEF" title="亿"></div>
                      <div class="amountLine--2qaEF" title="千"></div>
                      <div class="amountLine--2qaEF" title="百"></div>
                      <div class="amountLine--2qaEF" title="十"></div>
                      <div class="amountLine--2qaEF" title="万"></div>
                      <div class="amountLine--2qaEF" title="千"></div>
                      <div class="amountLine--2qaEF" title="百"></div>
                      <div class="amountLine--2qaEF" title="十"></div>
                      <div class="amountLine--2qaEF" title="元"></div>
                      <div class="amountLine--2qaEF" title="角"></div>
                      <div class="amountLine--2qaEF" title="分"></div>
                    </div>
                  </div>
                </td>
              </tr>
              </tfoot>
            </table>
          </div>

          <div class="wimoor-voucher-footer">
            <div class="voucher-signatures">
              <div class="signature-item">
                <span class="signature-label">制单人:</span>
                <span>{{ form.preparerBy }}</span>
                <el-button type="text" size="small" title="选择用户">
                  <el-icon><User /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-form>


      <el-dialog
          v-model="showSummaryDialog"
          title="常用摘要"
          width="500px"
          @open="loadCommonSummaries"
      >
        <div style="margin-bottom: 12px; display: flex; gap: 8px;">
          <el-input v-model="newSummaryText" placeholder="输入新摘要" size="small" style="flex: 1" @keyup.enter="handleAddCommonSummary" />
          <el-button type="primary" size="small" @click="handleAddCommonSummary">添加</el-button>
        </div>
        <div class="summary-list" v-loading="summaryLoading" style="max-height: 300px; overflow-y: auto;">
          <div
              v-for="item in commonSummaries"
              :key="item.id"
              class="summary-item"
              style="display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; cursor: pointer; border-bottom: 1px solid #f0f0f0;"
              @click="selectSummary(currentSummaryIndex, item.summary)"
          >
            <span>{{ item.summary }}</span>
            <el-button link type="danger" size="small" @click.stop="handleDeleteCommonSummary(item)">删除</el-button>
          </div>
          <div v-if="commonSummaries.length === 0 && !summaryLoading" style="text-align: center; color: #999; padding: 20px;">
            暂无常用摘要，请添加
          </div>
        </div>
      </el-dialog>
      <el-dialog
          v-model="showAuxiliaryDialog"
          title="辅助核算"
          width="500px"
          :close-on-click-modal="false"
          :close-on-press-escape="false"
          :show-close="true"
          :before-close="handleAuxiliaryDialogBeforeClose"
      >
        <div v-if="currentAuxiliaryItem">
          <div v-for="(setting, sIndex) in currentAuxiliaryItem.auxiliaryTypes" :key="sIndex" style="margin-bottom:12px;">
            <div style="display:flex;align-items:center;">
              <span style="width:80px;font-weight:bold;">{{ setting.typeName }}：</span>
              <el-select
                  v-model="setting.selectedItemId"
                  placeholder="请选择"
                  filterable
                  style="flex:1;"
              >
                <el-option
                    v-for="auxItem in setting.items"
                    :key="auxItem.itemId"
                    :label="auxItem.itemCode + '_' + auxItem.itemName"
                    :value="auxItem.itemId"
                ></el-option>
              </el-select>
            </div>
          </div>
        </div>
        <template #footer>
          <el-button type="primary" @click="confirmAuxiliary">确定</el-button>
        </template>
      </el-dialog>
      <UploadDialog ref="fileUpload" @change="handleFilesChange" type="finance" :file-type="['pdf','jpg','png']"></UploadDialog>
      <ImportVoucherDialog ref="importVoucherDialog"></ImportVoucherDialog>

      <!-- 凭证模版组件 -->
      <VoucherTemplate
        ref="voucherTemplateRef"
        :account-list="accountList"
        :entries="data.form.entries"
        :get-chinese-amount-digits="getChineseAmountDigits"
        :calculate-total="calculateTotal"
        @load-template="handleLoadTemplate"
      />
    </div>
  </div>

</template>

<script setup name="VouchersEdit">
import {
  listVouchers,
  getVouchers,
  addVouchers,
  updateVouchers,
  nextVoucherNo,
  updateVouchersFiles
} from "@/api/finance/vouchers"
import { ref, reactive, toRefs, onMounted,nextTick, computed } from "vue"
import { Search, Upload, User, Delete, Download ,MoreFilled} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import {listAll} from "@/api/finance/subjects.js";
import {listSubjectAuxiliarySetting, listAuxiliaryItems} from "@/api/finance/subject_auxiliary.js";
import finStore from "@/hooks/store/useFinanceStore.js"
import UploadDialog from "@/components/FileUpload/UploadDialog.vue";
import ImportVoucherDialog from './components/ImportVoucherDialog.vue';
import {listCurrency} from "@/api/finance/currency"
import {listVoucherTypes} from "@/api/finance/voucher_type"
import {getCurrentPeriod} from "@/api/finance/periods"
import {listVoucherSummary,addVoucherSummary,delVoucherSummary} from "@/api/finance/voucher_summary"
import VoucherTemplate from './components/VoucherTemplate.vue'
import $ from 'jquery';
import PrinterBtn from "@/components/header/printer.vue"; // Local import
import closedStampImg from "@/assets/image/pages/finance/closed.png";
import { Edit } from '@element-plus/icons-vue'
const router = useRouter()
const route = useRoute()
// const { proxy } = getCurrentInstance()
// 使用标准API替代proxy
const showMessage = (message, type = 'info') => {
  ElMessage({ message, type })
}
const importVoucherDialog=ref();
const showSuccess = (message) => showMessage(message, 'success')
const showWarning = (message) => showMessage(message, 'warning')
const showError = (message) => showMessage(message, 'error')

// 凭证模版组件引用
const voucherTemplateRef = ref(null)

const showAlert = (options) => {
  return ElMessageBox.alert(options.message, options.title || '提示', {
    confirmButtonText: '确定',
    callback: options.callback
  })
}

async function loadCurrency() {
  let params = {"groupid": await finStore.getCurrentTenantId(),"pageNum":1,"pageSize":1000};
  listCurrency(params).then(res => {
    data.currencyList = res.rows;
  });
}
async function loadVoucherTypes() {
  let params = {"groupid": await finStore.getCurrentTenantId(),"pageNum":1,"pageSize":100};
  listVoucherTypes(params).then(res => {
    data.voucherTypeList = res.rows;
  });
}

// 加载当前会计期间
async function loadCurrentPeriod() {
  try {
    let groupid = await finStore.getCurrentTenantId();
    const res = await getCurrentPeriod(groupid);
    if (res && res.data) {
      data.currentPeriod = res.data;
    }
  } catch (error) {
    console.error('获取当前会计期间失败:', error);
  }
}

const fileUpload=ref();
const isBalance = ref(true)
const accountList = ref([]) // 会计科目列表
const showSummaryDialog = ref(false) // 摘要选择对话框
const currentSummaryIndex = ref(-1) // 当前摘要输入框索引
const showAuxiliaryDialog = ref(false)
const currentAuxiliaryItem = ref(null)
const commonSummaries = ref([]) // 常用摘要（从数据库加载）
const newSummaryText = ref('') // 新增摘要输入
const summaryLoading = ref(false)

const vouchersRef = ref(null) // 表单引用
const data = reactive({
  form: {
    fileds:0,
    voucherId: null,
    tenantId: null,
    voucherType: '记', // 凭证字
    voucherNo: null,
    voucherDate: new Date().toISOString().split('T')[0], // 默认当前日期
    attachmentCount: 0,
    totalAmount: 0,
    debitTotal:0,
    creditTotal:0,
    debitTotalChinese:{},
    creditTotalChinese:{},
    voucherStatus: 3, // 默认状态，后台自动审核过账
    preparerBy: null,
    auditorId: null,
    postUserId: null,
    postTime: null,
    createdTime: null,
    updatedTime: null,
    files:[],
    entries: [
      { summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') },
      { summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') },
      { summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') },
      { summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') }
    ]
  },
  lastVoucherYearMonth: null, // 初始化为null，确保首次调用时请求后台
  queryParams: {
    pageNum: 0,
    pageSize: 1,
    voucherNo: null,
    voucherDate: null,
    attachmentCount: null,
    totalAmount: null,
    voucherStatus: null,
    preparerBy: null,
    auditorId: null,
    postTime: null,
    createdTime: null,
    updatedTime: null
  },
  total:0,
  rules:{
    voucherNo: [
      { required: true, message: "凭证编号不能为空", trigger: "blur" }
    ],
    voucherDate: [
      { required: true, message: "凭证日期不能为空", trigger: "blur" }
    ],
    totalAmount: [
      { required: true, message: "凭证总金额不能为空", trigger: "blur" },
      { type: 'number', min: 0, message: '凭证总金额不能为负数', trigger: 'blur' }
    ],
    preparerBy: [
      { required: true, message: "制单人不能为空", trigger: "blur" }
    ],
  },
  "widthStyle":"width:60%",
    "hasCurrency": false,
    "hasQuantity":false,
    "hasAuxiliary":false,
    "currencyList":[],
    "auxiliarySettings":[],
    "auxiliaryTypes":[],
    "auxiliaryItems":[],
    "voucherTypeList":[],
    "currentPeriod":null,
    "periodClosed":false
})

const { queryParams, total,form, rules,subjects,widthStyle,hasCurrency,currencyList,hasQuantity,hasAuxiliary,voucherTypeList } = toRefs(data)

// 添加ref数组存储输入元素引用
const summaryInputs = ref([]);
const accountSelects = ref([]);
const quantityInputs = ref([]);
const exchangeRateInputs = ref([]);
const auxiliarySelects = ref([]);

// 添加处理函数
function handleSummaryClick(index) {
  const entries = data.form.entries;
  
  // 如果不是第一行，且上一行有摘要，当前行没有摘要，则自动携带
  if (index > 0 && entries[index - 1] && entries[index - 1].summary && entries[index] && !entries[index].summary) {
    entries[index].summary = entries[index - 1].summary;
  }
  
  clearEdit();
  data.form.entries[index].rowEdit = true;
  nextTick(() => {
    if (summaryInputs.value[index]) {
      summaryInputs.value[index].focus();
    }
  });
}

function handleAccountClick(index) {
  clearEdit();
  data.form.entries[index].rowEdit = true;
  nextTick(() => {
    if (accountSelects.value[index]) {
      accountSelects.value[index].focus();

    }
  });
}

function handleCurrencyClick(index, item,type) {
  clearEdit();
  data.form.entries[index].rowEdit = true;
  nextTick(() => {
    if (data.hasQuantity) {
      // 数量/单价列
      if (type==="quantity"&&item.isQuantity && quantityInputs.value[index]) {
        quantityInputs.value[index].focus();
      }
    } else if (data.hasCurrency) {
      // 原币/汇率列
      if (type==="currency"&&item.isForeignCurrency && exchangeRateInputs.value[index]) {
        exchangeRateInputs.value[index].focus();
      }
    }
  });
}


// 页面加载时获取数据和初始化
onMounted(async () => {
  // 设置当前用户为制单人
  form.preparerBy = '当前用户'
  loadAccountList();
  loadCurrency();
  loadVoucherTypes();
  // 获取当前会计期间
  await loadCurrentPeriod();
  // 如果有voucherId参数，则加载凭证数据
  const voucherId = route.query?.voucherId
  if (voucherId) {
    getVoucherDetail(voucherId)
  }else{
    handleNextVoucherNo();
  }
  nextTick(()=>{
    document.addEventListener('click', function(event) {
      clearEdit();
    });
  })
})
function handleEditTrue(e,item,type,index){
  const evt = e || window.e || window.event;
  e.stopPropagation();
  clearEdit();
  item[type] = true;
  if(item){
  if (type == "debitEdit") {
    if (item.creditAmount) {
      item.debitAmount=item.creditAmount;
      item.creditAmount = "";
    }
     handleInputChange(item,"debitAmount");
  }
  if (type == "creditEdit") {
     if (item.debitAmount) {
      item.creditAmount=item.debitAmount;
      item.debitAmount = "";
    }
     handleInputChange(item,"creditAmount");
    }
  }
  nextTick(()=>{
    setTimeout(function(){
      if(type.indexOf("debit")>=0){
        $(".voucher-row").eq(index).find(".debit-col input").focus()
      }else{
        $(".voucher-row").eq(index).find(".credit-col input").focus()
      }
    },300)


  })
}

function handleCurrencyRate(item,type) {
   if(item.originalAmount&&item.exchangeRate){
       item[type]=item.originalAmount*item.exchangeRate;
    }
}
function handleInputChange(item, type) {
  if (type == null) {
    if (item.creditAmount) {
         type='creditAmount'
      } else {
        type = 'debitAmount';
      }
  }
  if (item.quantity && item.unitPrice) {
    let amount = 0;
      amount = item.quantity * item.unitPrice;
    if (item.exchangeRate != null) {
        item.originalAmount = amount;
        handleCurrencyRate(item,type);
    } else {
        item[type] = amount;
      }
  } else {
       handleCurrencyRate(item,type);
  }
  calculateTotal(item);

}
function handleFilesChange(files){
  if(data.form.voucherId){
    data.form.files = files;
    updateVouchersFiles(data.form).then(() => {
      showSuccess('上传成功');
      fileUpload.value.hide();
    })
  }else{
    data.form.files = files;
    fileUpload.value.hide();
  }

}
function handleImportVoucherShow(){
  importVoucherDialog.value.show();
}
async function handleQuery(){
  data.queryParams.groupid=await finStore.getCurrentTenantId();
  // 添加会计期间参数
  if (data.currentPeriod) {
    data.queryParams.startPeriod = data.currentPeriod.periodCode;
  }
  // endPeriod 使用当前日期的年月
  const now = new Date();
  data.queryParams.endPeriod = now.getFullYear().toString() + String(now.getMonth() + 1).padStart(2, '0');
  if(data.queryParams.pageNum>0){
     data.queryParams.isAsc="desc";
     data.queryParams.orderByColumn="created_time";
    listVouchers(data.queryParams).then(async res=>{
      data.total = res.total;
      data.hasQuantity = false;
      data.hasCurrency = false;
      data.hasAuxiliary = false;
      res.rows[0].entries.forEach(item=>{
        if(item.debitAmount&&!item.debitAmountChinese){
          item.debitAmountChinese=getChineseAmountDigits(item.debitAmount);
        }else{
          item.debitAmountChinese={};
        }
        if(item.creditAmount&&!item.creditAmountChinese){
          item.creditAmountChinese=getChineseAmountDigits(item.creditAmount);
        }else{
          item.creditAmountChinese={};
        }
        if (item.quantity) {
          item.isQuantity = true;
          data.hasQuantity=true;
        } else {
          item.isQuantity = false;
        }
        if (item.exchangeRate) {
          item.isForeignCurrency = true;
          data.hasCurrency = true;
        } else {
          item.isForeignCurrency = false;
        }
        if (item.auxiliaryList && item.auxiliaryList.length > 0) {
          item.isAuxiliary = true;
          data.hasAuxiliary = true;
        } else {
          item.isAuxiliary = false;
        }
      })

      Object.assign(data.form, res.rows[0])
      // 设置lastVoucherYearMonth，防止修改日期时重新请求凭证号
      if (data.form.voucherDate) {
        const voucherDate = new Date(data.form.voucherDate);
        data.lastVoucherYearMonth = `${voucherDate.getFullYear()}-${String(voucherDate.getMonth() + 1).padStart(2, '0')}`;
      }
      if(data.form.entries&&data.form.entries.length<4){
        for(let i=data.form.entries.length;i<4;i++){
          data.form.entries.push({ summary: '', account: '', debitAmount: '', creditAmount: '',creditAmountChinese:{},debitAmountChinese:{} })
        }
      }
      // 加载辅助核算设置并恢复选中状态
      for (const item of data.form.entries) {
        if (item.auxiliaryList && item.auxiliaryList.length > 0 && item.subjectId) {
          item.isAuxiliary = true;
          item.auxiliaryTypes = item.auxiliaryTypes || [];
          await loadAuxiliarySettings(item.subjectId, item);
        }
      }
      calculateTotal()
    })
  }else{
    await reset()
  }
}


// 获取凭证详情
async function getVoucherDetail(id) {
  let groupid=await finStore.getCurrentTenantId();
    listVouchers({"groupid":groupid,"voucherId":id}).then(async res=>{
      res.rows[0].entries.forEach(item=>{
        if(item.debitAmount&&!item.debitAmountChinese){
          item.debitAmountChinese=getChineseAmountDigits(item.debitAmount);
        }else{
          item.debitAmountChinese={};
        }
        if(item.creditAmount&&!item.creditAmountChinese){
          item.creditAmountChinese=getChineseAmountDigits(item.creditAmount);
        }else{
          item.creditAmountChinese={};
        }
      })
      Object.assign(data.form, res.rows[0])
      // 设置会计期间是否已结账状态
      data.periodClosed = res.rows[0].periodClosed || false;
      // 设置lastVoucherYearMonth，防止同一会计期间内修改日期时重新请求凭证号
      if (data.form.voucherDate) {
        const voucherDate = new Date(data.form.voucherDate);
        data.lastVoucherYearMonth = `${voucherDate.getFullYear()}-${String(voucherDate.getMonth() + 1).padStart(2, '0')}`;
      }
      if(data.form.entries&&data.form.entries.length<4){
        for(let i=data.form.entries.length;i<4;i++){
          data.form.entries.push({ summary: '', account: '', debitAmount: '', creditAmount: '',creditAmountChinese:{},debitAmountChinese:{} })
        }
      }
      // 加载辅助核算设置并恢复选中状态
      for (const item of data.form.entries) {
        if (item.auxiliaryList && item.auxiliaryList.length > 0 && item.subjectId) {
          item.isAuxiliary = true;
          item.auxiliaryTypes = item.auxiliaryTypes || [];
          await loadAuxiliarySettings(item.subjectId, item);
        }
      }
      calculateTotal()
  })
}

function changeCurrency(row){
  let rate=0;
  data.currencyList.forEach(item=>{
   if(item.code==row.currency){
     rate=item.rate;
   }
  })
  row.exchangeRate=rate;
  handleInputChange(row)
}
function clearEdit(){
  data.form.entries.forEach(item => {
    item.debitEdit = false;
    item.creditEdit = false;
    item.rowEdit=false;
  })
}
// 表单提交
function submitForm() {
  if (data.periodClosed) {
    showError('会计期间已结账，无法保存凭证');
    return;
  }
  vouchersRef.value.validate(async (valid) => {
    if (valid) {
      data.form.groupid=await finStore.getCurrentTenantId();
      data.form.voucherStatus = 3; // 后台自动审核过账，直接设为已过账状态
      if (data.form.voucherId != null) {
        updateVouchers(data.form).then(() => {
          showSuccess('修改成功')
        })
      } else {
        addVouchers(data.form).then(() => {
          showSuccess('新增成功')
        })
      }
    }
  })
}
async function handleNextVoucherNo(){
  // 获取当前日期的年月
  const currentDate = new Date(data.form.voucherDate);
  const currentYearMonth = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}`;
  
  // 如果年月没有变化，不需要重新请求（同一会计期间内修改日期，凭证编号不变）
  if (data.lastVoucherYearMonth === currentYearMonth) {
    return;
  }
  
  // 记录当前年月
  data.lastVoucherYearMonth = currentYearMonth;
  
  return nextVoucherNo({"voucherDate":data.form.voucherDate,
    "groupid":await finStore.getCurrentTenantId()}).then(response => {
    data.form.voucherNo = response.msg;
  })
}
// 保存并新增
function saveAndNew() {
  if (data.periodClosed) {
    showError('会计期间已结账，无法保存凭证');
    return;
  }
  vouchersRef.value.validate(async (valid) => {
    if (valid) {
      data.form.groupid=await finStore.getCurrentTenantId();
      data.form.voucherStatus = 3; // 后台自动审核过账，直接设为已过账状态
      if (data.form.voucherId != null) {
        await updateVouchers(data.form)
      } else {
        await addVouchers(data.form)
      }
      showSuccess('保存成功')
      await reset()
    }
  })
}

// 保存草稿
async function saveDraft() {
  if (data.periodClosed) {
    showError('会计期间已结账，无法保存凭证');
    return;
  }
  data.form.voucherStatus = 0 // 草稿状态
  data.form.groupid=await finStore.getCurrentTenantId();

  if (data.form.voucherId != null) {
    updateVouchers(data.form).then(() => {
      showSuccess('草稿保存成功')
    })
  } else {
    addVouchers(data.form).then(() => {
      showSuccess('草稿保存成功')
    })
  }
}

// 表单重置
async function reset() {
  data.hasCurrency=false;
  data.hasQuantity=false;
  data.hasAuxiliary=false;
  data.periodClosed=false;
  Object.assign(data.form, {
    voucherId: null,
    tenantId: null,
    voucherType: '记',
    voucherNo: null,
    voucherDate: data.form.voucherDate,
    attachmentCount: 0,
    totalAmount: 0,
    voucherStatus: 3,
    preparerBy: '当前用户',
    auditorId: null,
    postUserId: null,
    postTime: null,
    createdTime: null,
    updatedTime: null,
    // 添加总额中文数字的初始值
    debitTotalChinese: Array(11).fill(' '),
    creditTotalChinese: Array(11).fill(' '),
    files:[],
    entries: [
      { summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') },
      { summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') },
      { summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') },
      { summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') }
    ]
  })
  data.lastVoucherYearMonth = null; // 重置年月记录，确保重新请求
  await handleNextVoucherNo();
  calculateTotal()
}

// 计算借贷总额
function getChineseAmountDigits(amount) {
  // 修复空值检查逻辑，显式检查undefined和null，允许0值通过
  if (amount === undefined || amount === null || isNaN(amount) || typeof amount !== 'number') {
    return null;
  }

  // 处理负数
  const isNegative = amount < 0;
  const absoluteAmount = Math.abs(amount);

  // 转换为数字并固定两位小数
  const fixedAmount = absoluteAmount.toFixed(2);

  // 分割整数和小数部分
  const [integerPart, decimalPart] = fixedAmount.split('.');

  // 确保整数部分不超过9位，取最后9位并补空格
  const paddedInteger = integerPart.padStart(9, ' ').slice(-9);
  // 组合所有数字位（9位整数 + 2位小数）
  const allDigits = paddedInteger.split('').concat(decimalPart.split(''));

  // 确保数组始终有11个元素
  while (allDigits.length < 11) {
    allDigits.unshift(' ');
  }
  allDigits.splice(11); // 截断过长的数组

  return allDigits;
}
function calculateTotal(item) {
  try {
    if(item){
      if(item.debitAmount && !isNaN(parseFloat(item.debitAmount))){
        item.debitAmountChinese = getChineseAmountDigits(parseFloat(item.debitAmount)) || Array(11).fill(' ');
      }else{
        item.debitAmountChinese = Array(11).fill(' ');
      }
      if(item.creditAmount && !isNaN(parseFloat(item.creditAmount))){
        item.creditAmountChinese = getChineseAmountDigits(parseFloat(item.creditAmount)) || Array(11).fill(' ');
      }else{
        item.creditAmountChinese = Array(11).fill(' ');
      }
    }
    data.form.debitTotal = data.form.entries.reduce((total, item) => total + (parseFloat(item.debitAmount) || 0), 0)
    data.form.creditTotal = data.form.entries.reduce((total, item) => total + (parseFloat(item.creditAmount) || 0), 0)
    isBalance.value = Math.abs(data.form.debitTotal - data.form.creditTotal) < 0.01
    data.form.totalAmount = data.form.debitTotal;
    data.form.entries.forEach((item,index) => {
      item.entryNo=index+1;
    })
    data.form.debitTotalChinese = !isNaN(data.form.debitTotal) ? (getChineseAmountDigits(data.form.debitTotal) || Array(11).fill(' ')) : Array(11).fill(' ');
    data.form.creditTotalChinese = !isNaN(data.form.creditTotal) ? (getChineseAmountDigits(data.form.creditTotal) || Array(11).fill(' ')) : Array(11).fill(' ');
  } catch (error) {
    console.error('Error in calculateTotal:', error);
    // 确保在错误情况下仍有有效的默认值
    if(!data.form.debitTotalChinese) data.form.debitTotalChinese = Array(11).fill(' ');
    if(!data.form.creditTotalChinese) data.form.creditTotalChinese = Array(11).fill(' ');
    data.form.entries.forEach(item => {
      if(!item.debitAmountChinese) item.debitAmountChinese = Array(11).fill(' ');
      if(!item.creditAmountChinese) item.creditAmountChinese = Array(11).fill(' ');
    });
  }
}

// 格式化货币
function formatCurrency(value) {
  return Number(value || 0).toFixed(2)
}

// 添加行
// 添加行
function addItem() {
  data.form.entries.push({ summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') })
}

// 插入行
function insertRow(index) {
  data.form.entries.splice(index, 0, { summary: '', account: '', debitAmount: '', creditAmount: '', creditAmountChinese: Array(11).fill(' '), debitAmountChinese: Array(11).fill(' ') })
}

// 复制行
function copyRow(index) {
  const newRow = {...data.form.entries[index]};
  // 重置中文金额数组为11个空格元素
  newRow.debitAmountChinese = Array(11).fill(' ');
  newRow.creditAmountChinese = Array(11).fill(' ');
  // 清空金额值以避免复制原金额
  newRow.debitAmount = '';
  newRow.creditAmount = '';
  data.form.entries.splice(index + 1, 0, newRow);
  calculateTotal();
}

// 删除行
function removeItem(index) {
  if (data.form.entries.length > 1) {
    data.form.entries.splice(index, 1)
    calculateTotal()
  }
}

// 自动平衡
function autoBalance() {
  const diff = (data.form.debitTotal || 0) - (data.form.creditTotal || 0)
  if (Math.abs(diff) > 0.01) {
    // 如果最后一行只有一方有金额，则补充差额到另一方
    const lastItem = data.form.entries[data.form.entries.length - 1]
    if (lastItem.debit > 0 && lastItem.credit === 0) {
      lastItem.credit = diff
    } else if (lastItem.credit > 0 && lastItem.debit === 0) {
      lastItem.debit = -diff
    } else {
      // 否则创建新行来平衡
      data.form.entries.push({
        summary: '自动平衡',
        account: '',
        debitAmount: diff > 0 ? 0 : -diff,
        creditAmount: diff > 0 ? diff : 0 ,
        creditAmountChinese:{},
        debitAmountChinese:{}
      })
    }
    calculateTotal(data.form.entries[data.form.entries.length-1]);
  }
}

// 显示摘要建议
function showSummarySuggestions(index) {
  currentSummaryIndex.value = index
  showSummaryDialog.value = true
}

// 加载常用摘要
async function loadCommonSummaries() {
  summaryLoading.value = true
  try {
    const tenantId = await finStore.getCurrentTenantId();
    const response = await listVoucherSummary({ groupid: tenantId, pageSize: 2000, pageNum: 1 });
    if (response && response.code === 200 && response.rows) {
      commonSummaries.value = response.rows;
    } else {
      commonSummaries.value = [];
    }
  } catch (error) {
    console.error('Failed to load summaries:', error);
    commonSummaries.value = [];
  } finally {
    summaryLoading.value = false;
  }
}

// 添加常用摘要
async function handleAddCommonSummary() {
  if (!newSummaryText.value || !newSummaryText.value.trim()) {
    ElMessage({ message: '请输入摘要内容', type: 'warning' });
    return;
  }
  try {
    const tenantId = await finStore.getCurrentTenantId();
    const response = await addVoucherSummary({
      summary: newSummaryText.value.trim(),
      groupid: tenantId
    });
    if (response && response.code === 200) {
      ElMessage({ message: '添加成功', type: 'success' });
      newSummaryText.value = '';
      await loadCommonSummaries();
    } else {
      ElMessage({ message: '添加失败：' + (response.msg || '未知错误'), type: 'error' });
    }
  } catch (error) {
    ElMessage({ message: '请求失败：' + error.message, type: 'error' });
  }
}

// 删除常用摘要
async function handleDeleteCommonSummary(item) {
  try {
    await ElMessageBox.confirm('确定删除该摘要吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const response = await delVoucherSummary(item.id);
    if (response && response.code === 200) {
      ElMessage({ message: '删除成功', type: 'success' });
      await loadCommonSummaries();
    } else {
      ElMessage({ message: '删除失败：' + (response.msg || '未知错误'), type: 'error' });
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage({ message: '请求失败：' + error.message, type: 'error' });
    }
  }
}

// 选择摘要
function selectSummary(index, summary) {
  if (index >= 0 && index < data.form.entries.length) {
    data.form.entries[index].summary = summary
  }
  showSummaryDialog.value = false
}

// 复制摘要到下一行
function copySummaryToNext(index) {
  // 可以根据需要实现复制摘要到下一行的功能
}

// 焦点移动到下一个单元格
function focusNextCell(event, cellType, index) {
  event.preventDefault();
  
  const entries = data.form.entries;
  
  // 根据当前单元格类型决定下一步
  switch (cellType) {
    case 'summary':
      // 摘要列按回车，移动到同一行的科目列
      if (accountSelects.value[index]) {
        accountSelects.value[index].focus();
      }
      break;
    case 'account':
      // 科目列按回车，移动到同一行的借方金额列
      nextTick(() => {
        if (data.hasQuantity) {
          // 如果有数量列，先聚焦到数量输入框
          if (entries[index].isQuantity && quantityInputs.value[index]) {
            quantityInputs.value[index].focus();
          }
        } else {
          // 否则聚焦到借方金额输入框
          focusDebitInput(index);
        }
      });
      break;
    case 'debit':
      // 借方金额列按回车，移动到同一行的贷方金额列
      nextTick(() => {
        if (data.hasCurrency && entries[index].isForeignCurrency) {
          // 如果有外币列，先处理外币
          focusCreditInput(index);
        } else {
          focusCreditInput(index);
        }
      });
      break;
    case 'credit':
      // 贷方金额列按回车，移动到下一行的摘要列
      const nextIndex = index + 1;
      
      // 如果是最后一行，自动添加新行
      if (nextIndex >= entries.length) {
        addItem();
      }
      
      // 携带上一行的摘要到下一行
      if (entries[index] && entries[index].summary && entries[nextIndex] && !entries[nextIndex].summary) {
        entries[nextIndex].summary = entries[index].summary;
      }
      
      // 激活下一行的编辑状态并聚焦到摘要输入框
      clearEdit();
      entries[nextIndex].rowEdit = true;
      
      nextTick(() => {
        if (summaryInputs.value[nextIndex]) {
          summaryInputs.value[nextIndex].focus();
        }
      });
      break;
  }
}

function focusDebitInput(index) {
  // 聚焦到借方金额输入框
  nextTick(() => {
    const debitInput = document.querySelector(`.voucher-row:nth-child(${index + 1}) .debit-col input`);
    if (debitInput) {
      debitInput.focus();
    }
  });
}

function focusCreditInput(index) {
  // 聚焦到贷方金额输入框
  nextTick(() => {
    const creditInput = document.querySelector(`.voucher-row:nth-child(${index + 1}) .credit-col input`);
    if (creditInput) {
      creditInput.focus();
    }
  });
}

// 焦点移动到下一行
function focusNextRow(index) {
  // 如果是最后一行，自动添加新行
  if (index === form.entries.length - 1) {
    addItem()
  }
  // 可以根据需要实现行之间的焦点移动
}

function handleKeydown(event, index) {
  // 可以根据需要实现键盘快捷键处理
}

async function loadAccountList() {
  // 模拟加载会计科目数据
  if (accountList.value.length === 0) {
    // 实际项目中应该从API获取
    listAll({"groupid":await finStore.getCurrentTenantId(),"status":1}).then(res=>{
      if(res.data && res.data.length>0){
        accountList.value = res.data;
      }
    });
  }
}
function handleAccountChange(item) {
  accountList.value.forEach(account => {
    if (account.subjectId === item.subjectId) {
        item.selectAccount=account;
        item.subjectCode=account.subjectCode;
        item.subjectName=account.subjectName;
    }  
  });
  if(item.selectAccount){
     if(item.selectAccount.quantity){
        item.isQuantity = true;
     } else {
        item.isQuantity = false;
    }
     if(item.selectAccount.foreignCurrency){
       item.isForeignCurrency = true;
     } else {
       item.isForeignCurrency = false;
     }
     if(item.selectAccount.isAuxiliary){
       item.isAuxiliary = true;
       item.auxiliaryText = '';
       loadAuxiliarySettings(item.subjectId, item).then(() => {
         currentAuxiliaryItem.value = item;
         showAuxiliaryDialog.value = true;
       });
     } else {
       item.isAuxiliary = false;
       item.auxiliaryTypes = [];
       item.auxiliaryItems = [];
       item.auxiliaryText = '';
     }
  }else{
    item.isQuantity = false;
    item.isForeignCurrency = false;
    item.isAuxiliary = false;
    item.auxiliaryTypes = [];
    item.auxiliaryItems = [];
    item.auxiliaryText = '';
  }
   let hasQuantity=false;
   let hasCurrency=false;
   let hasAuxiliary=false;
   data.form.entries.forEach((item,index) => {
      if(item.isQuantity  ){
          hasQuantity=true;
      }
      if(item.isForeignCurrency){
        hasCurrency=true;
      }
      if(item.isAuxiliary){
        hasAuxiliary=true;
      } 
   });
  data.hasCurrency = hasCurrency;
  data.hasQuantity = hasQuantity;
  data.hasAuxiliary = hasAuxiliary; 
}

async function loadAuxiliarySettings(subjectId, item) {
  try {
    const res = await listSubjectAuxiliarySetting(subjectId);
    if (res.rows && res.rows.length > 0) {
      item.auxiliaryTypes = res.rows;
      // 顺序加载每个类型的项目列表
      for (const setting of item.auxiliaryTypes) {
        await loadAuxiliaryItems(setting, item);
        // 恢复已选中的辅助核算项
        if (item.auxiliaryList && item.auxiliaryList.length > 0) {
          const found = item.auxiliaryList.find(a => String(a.auxiliaryTypeId) === String(setting.auxiliaryTypeId));
          if (found) {
            setting.selectedItemId = Number(found.auxiliaryItemId);
          }
        }
      }
      // 恢复显示文本
      if (item.auxiliaryList && item.auxiliaryList.length > 0) {
        const parts = [];
        item.auxiliaryTypes.forEach(setting => {
          const name = getAuxiliaryItemName(setting);
          if (name) parts.push(name);
        });
        item.auxiliaryText = parts.join(' - ');
      }
    } else {
      item.auxiliaryTypes = [];
    }
  } catch (e) {
    item.auxiliaryTypes = [];
  }
}

async function loadAuxiliaryItems(setting, item) {
  try {
    const groupid = await finStore.getCurrentTenantId();
    const params = { typeId: setting.auxiliaryTypeId, status: 1, groupid: groupid };
    const res = await listAuxiliaryItems(params);
    if (res.rows && res.rows.length > 0) {
      setting.items = res.rows;
    } else {
      setting.items = [];
    }
  } catch (e) {
    setting.items = [];
  }
}

function getAuxiliaryItemName(setting) {
  if (setting.selectedItemId && setting.items) {
    const found = setting.items.find(i => String(i.itemId) === String(setting.selectedItemId));
    return found ? found.itemCode + '_' + found.itemName : '';
  }
  return '';
}

function confirmAuxiliary() {
  if (currentAuxiliaryItem.value && currentAuxiliaryItem.value.auxiliaryTypes) {
    const hasEmpty = currentAuxiliaryItem.value.auxiliaryTypes.some(setting => !setting.selectedItemId);
    if (hasEmpty) {
      showWarning('所有辅助核算项目均为必填，请填写完整');
      return;
    }
    const parts = [];
    const auxiliaryList = [];
    currentAuxiliaryItem.value.auxiliaryTypes.forEach(setting => {
      const name = getAuxiliaryItemName(setting);
      if (name) {
        parts.push(name);
      }
      if (setting.selectedItemId) {
        auxiliaryList.push({
          auxiliaryTypeId: setting.auxiliaryTypeId,
          auxiliaryItemId: setting.selectedItemId
        });
      }
    });
    currentAuxiliaryItem.value.auxiliaryText = parts.join(' - ');
    currentAuxiliaryItem.value.auxiliaryList = auxiliaryList;
  }
  showAuxiliaryDialog.value = false;
  currentAuxiliaryItem.value = null;
}

function openAuxiliaryDialog(item) {
  if (item.auxiliaryTypes && item.auxiliaryTypes.length > 0) {
    currentAuxiliaryItem.value = item;
    showAuxiliaryDialog.value = true;
  } else {
    loadAuxiliarySettings(item.subjectId, item).then(() => {
      currentAuxiliaryItem.value = item;
      showAuxiliaryDialog.value = true;
    });
  }
}

function handleAuxiliaryDialogBeforeClose(done) {
  if (currentAuxiliaryItem.value && currentAuxiliaryItem.value.auxiliaryTypes) {
    const hasEmpty = currentAuxiliaryItem.value.auxiliaryTypes.some(setting => !setting.selectedItemId);
    if (hasEmpty) {
      showWarning('所有辅助核算项目均为必填，请填写完整后再关闭');
      return;
    }
  }
  currentAuxiliaryItem.value = null;
  done();
}

function queryAccount(query) {
  // 可以根据需要实现远程搜索功能

}

function showAccountSelector(index) {
  // 可以根据需要实现账户选择器功能
}

function showCalculator(type, index) {
  // 可以根据需要实现计算器功能
}

function uploadAttachment() {
  // 可以根据需要实现附件上传功能
  fileUpload.value.show(data.form.files);
}

function searchVoucher() {
  // 可以根据需要实现凭证搜索功能
}

function handleDropdownCommand(command) {
  if (command === 'saveAsTemplate') {
    openSaveTemplateDialog()
  } else if (command === 'createFromTemplate') {
    openFromTemplateDialog()
  }
}

function handlePreferences(command) {
  // 可以根据需要实现偏好设置功能
}

function showKeyboardShortcuts() {
  showAlert({
    message: 'F1: 帮助\nF2: 保存\nF3: 新增\nF5: 刷新',
    title: '快捷键说明'
  })
}

function toggleFullScreen() {
  // 可以根据需要实现全屏功能
  if(data.widthStyle==="width:100%"){
    data.widthStyle="width:60%";
  }else{
    data.widthStyle="width:100%";
  }
}



function prevVoucher() {
  // 可以根据需要实现切换到上一张凭证的功能
    data.queryParams.pageNum=data.queryParams.pageNum+1;
    handleQuery();
}

function nextVoucher() {
  // 可以根据需要实现切换到下一张凭证的功能
  data.queryParams.pageNum=data.queryParams.pageNum-1;
  handleQuery();
}

// ==================== 凭证模版相关函数 ====================

// 处理从模版加载凭证
function handleLoadTemplate(newEntries) {
  data.form.entries = newEntries
  calculateTotal()
}

// 打开从模版生成凭证对话框
function openFromTemplateDialog() {
  voucherTemplateRef.value.openFromTemplateDialog()
}

// 打开保存为凭证模版对话框
function openSaveTemplateDialog() {
  voucherTemplateRef.value.openSaveTemplateDialog()
}

</script>
<style>
/*   .wimoor-voucher-table-wrapper .wimoor-voucher-table tr:hover{
     
  }
  .wimoor-voucher-table-wrapper .wimoor-voucher-table td:hover{
     
  } */
.wimoor-voucher-table .account-col .el-input__wrapper ,
.wimoor-voucher-table .summary-col .el-input__wrapper,
.wimoor-voucher-table .amount-input-wrapper .el-input__wrapper {
  box-shadow: none !important;
}
.wimoor-voucher-table .account-col .el-select__wrapper ,
.wimoor-voucher-table .summary-col .el-select__wrapper,
.wimoor-voucher-table .amount-input-wrapper .el-select__wrapper {
  box-shadow: none !important;
}
.wimoor-voucher-table .account-col .el-select__input,
.wimoor-voucher-table .summary-col .el-select__input,
.wimoor-voucher-table .amount-input-wrapper.el-select__input{
  height:50px;
}
.wimoor-voucher-table  .currency-col{
  font-size:12px;
}

</style>
<style scoped>
.seal-watermark {
  position: absolute;
  top: 10px;
  right: 20%;
  width: 120px;
  height: auto;
  pointer-events: none;
  z-index: 100;
  transform: rotate(-15deg);
  opacity: 0.75;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  margin-bottom: 10px;
  font-family: 'Microsoft YaHei', Arial, sans-serif;
}

.action-right {
  display: flex;
  gap: 10px;
}

.voucher-header {
  border: 1px solid #d0d0d0;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  padding: 15px;
  font-family: 'Microsoft YaHei', Arial, sans-serif;
}

.voucher-main-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.voucher-type-info {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.voucher-type-label,
.voucher-no-label,
.voucher-date-label {
  margin-right: 5px;
  color: var(--el-text-color-regular);
  font-weight: 500;
}

.voucher-period-info {
  text-align: center;
}

.voucher-period-info h2 {
  margin: 0 0 5px 0;
  font-size: 18px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.voucher-period {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.voucher-attachment-info {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
}

.wimoor-voucher-table-wrapper {
  border: 1px solid #000000;
}

.wimoor-voucher-table {
  width: 100%;
  border-collapse: collapse;
  font-family: 'Microsoft YaHei', Arial, sans-serif;
}

.wimoor-voucher-table th,
.wimoor-voucher-table td {
  border: 1px solid #cacaca;
}

.wimoor-voucher-table th {
  font-weight: 500;
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.row-number-col {
  width: 80px;
  text-align: center;
}

.summary-col {
  width: 200px;
}

.account-col {
  width: 250px;
}

.debit-col,
.credit-col {
  width: 180px;
  text-align: center;
}

.amount-header {
  font-size: 13px;
  margin-bottom: 5px;
}

.amount-digits {
  display: flex;
  justify-content: space-around;
  font-size: 10px;
  color: #606266;
}

.voucher-row {
  height: 40px;
}
.voucher-row .row-actions {
  display: none;
  justify-content: center;
  gap: 5px;
  margin-bottom: 2px;
}
.voucher-row:hover .row-actions {
  display: flex;
  justify-content: center;
  gap: 5px;
  margin-bottom: 2px;
}
.voucher-row .row-number-text {
  text-align: center;
  font-size: 13px;
  display: block;
  color: var(--el-text-color-regular);
}
.voucher-row:hover .row-number-text {
  text-align: center;
  font-size: 13px;
  display: none;
  color: var(--el-text-color-regular);
}
.wimoor-input,
.wimoor-select {
  border-radius: 0 !important;
}

.wimoor-input:focus,
.wimoor-select:focus {
  box-shadow: none !important;
  border-color: #409eff !important;
}

.summary-suggest-btn,
.account-select-btn {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  padding: 0 5px;
}

.amount-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.amount-calc-btn {
  position: absolute;
  right: 0;
  padding: 0 5px;
}

.total-row {
  font-weight: 500;
}

.total-label {
  text-align: right;
  font-size: 14px;
  color: var(--el-text-color-primary);
  padding-right: 15px;
}

.total-amount-cell {
  text-align: right;
  font-size: 14px;
  color: var(--el-text-color-primary);
  padding-right: 15px;
  font-weight: bold;
}

.wimoor-voucher-footer {
  border: 1px solid #d0d0d0;
  border-top: none;
  border-radius: 0 0 4px 4px;
  padding: 12px 15px;
  font-family: 'Microsoft YaHei', Arial, sans-serif;
}

.voucher-signatures {
  display: flex;
  gap: 30px;
  align-items: center;
}

.signature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.signature-label {
  font-weight: 500;
  color: var(--el-text-color-primary);
  min-width: 70px;
  font-size: 13px;
}

/* 会计科目选项样式 */
.account-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
}

.account-option span:nth-child(1) {
  color: #409eff;
  font-weight: 500;
  margin-right: 10px;
  min-width: 80px;
}

.account-option span:nth-child(2) {
  flex: 1;
}

.account-balance {
  color: #67c23a;
  font-size: 12px;
}

/* 摘要选择对话框样式 */
.summary-list {
  max-height: 300px;
  overflow-y: auto;
}

.summary-item {
  padding: 10px 15px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.summary-item:hover {
}

.summary-item:last-child {
  border-bottom: none;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .wimoor-voucher-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .voucher-main-info {
    flex-wrap: wrap;
    gap: 20px;
    width: 100%;
  }

  .voucher-period-info {
    width: 100%;
    text-align: center;
  }

  .voucher-attachment-info {
    width: 100%;
    justify-content: flex-end;
  }
}

.amountLineBgWrapper--1MZC6 {
  position: relative;
  width: 100%;
  height: 100%;
  font-size: 16px;
  font-weight: 700
}

.amountLineBgWrapper--1MZC6.red--3rLFi {
  color: #fb2323;
}

.amountLineBgWrapper--1MZC6 .trillionAmount--1wIli {
  height: 100%;
  text-align: right;
  flex-direction: row;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 4px;
  letter-spacing: 2px
}

.amountLineBgWrapper--1MZC6 .amountValueWrapper--2cduc {
  position: absolute;
  left: 6px;
  top: 0;
  width: 100%;
  letter-spacing: 9px;
  height: 100%;
  z-index: 11;
  display: flex;
  justify-content: flex-end;
  align-items: center
}

.amountLineBgWrapper--1MZC6 .amountLine--2qaEF {
  float: left;
  height: 100%;
  width: 9.0909%;
  position: relative;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center
}

.amountLineBgWrapper--1MZC6 .amountLine--2qaEF.amountLineActive--1i4tk {
  background-color: #5582f3;
  color: #fff
}

.amountLineBgWrapper--1MZC6 .amountLine--2qaEF+.amountLine--2qaEF {
  border-left: 1px solid #e7e9eb
}

.amountLineBgWrapper--1MZC6 .amountLine--2qaEF:nth-of-type(10) {
  border-color: rgba(255,95,31,.3)
}

.amountLineBgWrapper--1MZC6 .amountLine--2qaEF:nth-of-type(10):after {
  content: "";
  position: absolute;
  bottom: -3px;
  left: -1px;
  height: 2px;
  width: 1px;
  background-color: rgba(255,95,31,.3)
}

.amountLineBgWrapper--1MZC6 .amountLine--2qaEF:nth-of-type(4),.amountLineBgWrapper--1MZC6 .amountLine--2qaEF:nth-of-type(7) {
  border-color: rgba(47,149,255,.3)
}

.amountLineBgWrapper--1MZC6 .amountLine--2qaEF:nth-of-type(4):after,.amountLineBgWrapper--1MZC6 .amountLine--2qaEF:nth-of-type(7):after {
  content: "";
  position: absolute;
  bottom: -3px;
  left: -1px;
  height: 2px;
  width: 1px;
  background-color: rgba(47,149,255,.3)
}

.amountHeaderWrapper--3TtBf {
  height: 50px;
  position: relative
}

.amountHeaderWrapper--3TtBf .amountTitle--146pZ {
  height: 50%;
  line-height: 27px;
  text-align: center;
  font-weight: 600
}

.amountHeaderWrapper--3TtBf .amountUnit--XSR71 {
  border-top: 1px solid #e7e9eb;
  height: 50%;
  line-height: 23px;
  text-align: center;
  padding-left: 1px
}

.amountHeaderWrapper {
  height: 50px;
  position: relative
}

.amountHeaderWrapper .amountTitle--146pZ {
  height: 50%;
  line-height: 27px;
  text-align: center;
  font-weight: 600
}

.amountHeaderWrapper .amountUnit--XSR71 {
  height: 50%;
  line-height: 23px;
  text-align: center;
  padding-left: 1px
}

.customHeaderWrapper--3Gf3Q {
  height: 100%;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center
}

.customHeaderWrapper--3Gf3Q .customTitle--3V-lJ {
  width: 100%;
  text-align: center;
  font-weight: 600
}

.customHeaderWrapper--3Gf3Q .customTitleOpt--2TI73 {
  position: absolute;
  top: 18px;
  right: 16px;
  margin-left: 12px
}

.customHeaderWrapper--3Gf3Q .customTitleOptBtn--3bFEA {
  color: #5582f3;
  font-size: 12px;
  font-weight: 400;
  cursor: pointer
}
.ql-bg-purple{
  background-color: #dedede;
  height:calc(100vh - 85px);
}

/* 暗黑模式适配 */
:global(.dark) .voucher-body {
  background-color: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

:global(.dark) .voucher-header,
:global(.dark) .wimoor-voucher-table-wrapper,
:global(.dark) .wimoor-voucher-footer {
  background-color: var(--el-bg-color);
  color: var(--el-text-color-primary);
  border-color: var(--el-border-color);
}

:global(.dark) .wimoor-voucher-table th,
:global(.dark) .wimoor-voucher-table td {
  border-color: var(--el-border-color);
}

:global(.dark) .summary-item {
  border-bottom-color: var(--el-border-color-lighter);
}

:global(.dark) .summary-item:hover {
  background-color: var(--el-fill-color-light);
}
 
</style>
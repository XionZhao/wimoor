
<template>
  <div class="deepseek-app" :class="{ 'drawer-mode': drawerMode }">
    <el-row v-if="innerType=='deepseek'" class="deepseek-row">
      <!-- 左侧会话列表 -->
      <el-col v-if="!drawerMode" :span="4" class="sidebar-left">
        <div class="sidebar-header">
          <el-button type="primary" class="new-session-btn" @click="handleAddSession()">
            <el-icon><Plus /></el-icon>
            <span>新建会话</span>
          </el-button>
        </div>
        <el-scrollbar height="calc(100vh - 160px)" class="session-scrollbar">
          <div class="session-list">
            <div v-if="sessions.length === 0" class="empty-state">
              <div class="empty-icon">💬</div>
              <div class="empty-text">暂无会话</div>
              <div class="empty-subtitle">点击上方按钮创建新会话</div>
            </div>
            <div
              v-for="item in sessions"
              :key="item.id"
              class="session-item"
              :class="{ 'active': sessionid === item.id }"
              @click="handleSession(item)"
            >
              <div class="session-item-content">
                <div class="session-title">{{ item.title || '新会话' }}</div>
              </div>
              <div class="session-item-actions">
                <el-button 
                  type="danger" 
                  size="small" 
                  circle 
                  @click.stop="handleDeleteSession(item)"
                  title="删除会话"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </el-col>

      <!-- 中间聊天区域 -->
      <el-col :span="drawerMode ? 24 : 16" class="chat-main">
        <div ref="messagesWrapperRef" class="messages-wrapper">
          <template v-for="(item, index) in messages" :key="item.id || item.message_id || index">
            <div class="message-wrapper" :class="item.role=='user' ? 'user-message' : 'assistant-message'">
              <div class="message-avatar">
                <div v-if="item.role=='user'" class="avatar user-avatar">你</div>
                <div v-else class="avatar ai-avatar">AI</div>
              </div>
              <div class="message-content">
                <!-- 思考过程展示 -->
                <div v-if="item.reasoning" class="reasoning-block">
                  <div class="reasoning-header" @click="item.showReasoning = !item.showReasoning">
                    <span class="reasoning-icon">💭</span>
                    <span class="reasoning-title">{{ item.isThinking ? '正在思考...' : '思考完成' }}</span>
                    <span class="reasoning-toggle">{{ item.showReasoning ? '▼' : '▶' }}</span>
                  </div>
                  <div v-if="item.showReasoning" class="reasoning-content">
                    <MarkdownRenderer :content="item.reasoning" :showCodeOnly="true"></MarkdownRenderer>
                  </div>
                </div>
                <!-- 正式内容 -->
                <div class="message-bubble" :class="item.role=='user' ? 'user-bubble' : 'ai-bubble'">
                  <MarkdownRenderer :content="item.content" :showCodeOnly="true"></MarkdownRenderer>
                  <span v-if="item.isStreaming && !item.isThinking" class="streaming-cursor">|</span>
                </div>
              </div>
            </div>
          </template>
          <!-- 流式消息占位 -->
          <div id="streaming-container" class="streaming-container"></div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <div class="input-wrapper">
            <div class="textarea-container">
              <textarea
                v-model="message"
                placeholder="输入消息... (Enter发送，Shift+Enter换行)"
                @keydown.enter.exact="handleSubmit"
                rows="3"
                ref="textareaRef"
                class="message-textarea"
              />
            </div>
            <div class="input-actions">
              <div class="model-select">
                <el-select size="small" v-model="search_model" placeholder="选择模型" class="model-dropdown">
                  <el-option label="qwen-turbo" value="qwen-turbo">通义千问-Turbo (最快)</el-option>
                  <el-option label="qwen-plus" value="qwen-plus">通义千问-Plus (均衡)</el-option>
                  <el-option label="qwen-max" value="qwen-max">通义千问-Max (最强)</el-option>
                  <el-option label="deepseek-v3.2" value="deepseek-v3.2">DeepSeek-阿里云</el-option>
                  <el-option label="siliconflow/deepseek-v3.2" value="siliconflow/deepseek-v3.2">DeepSeek-硅基流动</el-option>
                  <el-option label="vanchin/deepseek-v3.2-think" value="vanchin/deepseek-v3.2-think">DeepSeek-快手万擎</el-option>
                  <el-option label="kimi-k2-thinking" value="kimi-k2-thinking">Kimi-阿里云</el-option>
                  <el-option label="kimi/kimi-k2.5" value="kimi/kimi-k2.5">Kimi-月之暗面</el-option>
                  <el-option label="MiniMax-M2.5" value="MiniMax-M2.5">MiniMax-阿里云</el-option>
                  <el-option label="MiniMax/MiniMax-M2.7" value="MiniMax/MiniMax-M2.7">MiniMax-稀宇科技</el-option>
                </el-select>
                <el-tooltip content="Agent模式：AI自动调用系统接口查询数据，帮您解决业务问题" placement="top">
                  <el-switch
                    v-model="agentMode"
                    class="agent-switch"
                    inline-prompt
                    active-text="Agent"
                    inactive-text="普通"
                  />
                </el-tooltip>
                <el-tooltip v-if="!agentMode" content="流式读取：实时显示AI回复，响应更快" placement="top">
                  <el-switch
                    v-model="streamEnabled"
                    class="stream-switch"
                    inline-prompt
                    active-text="流"
                    inactive-text="普通"
                  />
                </el-tooltip>
              </div>
              <el-button
                type="primary"
                :loading="isLoading"
                @click="handleSubmit"
                :disabled="!message.trim()"
                class="send-btn"
              >
                <el-icon v-if="!isLoading"><Promotion /></el-icon>
                <span>{{ isLoading ? '发送中...' : '发送' }}</span>
              </el-button>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧快捷短语 -->
      <el-col v-if="!drawerMode" :span="4" class="sidebar-right">
        <div class="sidebar-header">
          <div class="sidebar-title">快捷短语</div>
        </div>
        <el-scrollbar height="calc(100vh - 160px)">
          <div class="quick-phrases">
            <div v-if="searchKeys.length === 0" class="empty-state">
              <div class="empty-icon">💡</div>
              <div class="empty-text">暂无短语</div>
            </div>
            <div
              v-for="item in searchKeys"
              :key="item.id"
              class="phrase-item"
              @click="handleContent(item.content)"
            >
              <div class="phrase-text">{{ item.content }}</div>
            </div>
          </div>
        </el-scrollbar>
      </el-col>
    </el-row>
	     <el-col v-if="innerType=='product'" :span="24" style="display: flex; flex-direction: column; height: 100%; overflow: hidden;">
	        <div ref="messagesWrapperRef" class="messages-wrapper product-messages-wrapper">
	          <template v-for="(item, index) in messages" :key="item.id || item.message_id || index">
	            <div class="message-wrapper" :class="item.role=='user' ? 'user-message' : 'assistant-message'">
	              <div class="message-avatar">
	                <div v-if="item.role=='user'" class="avatar user-avatar">你</div>
	                <div v-else class="avatar ai-avatar">AI</div>
	              </div>
	              <div class="message-content">
	                <!-- 思考过程展示 -->
	                <div v-if="item.reasoning" class="reasoning-block">
	                  <div class="reasoning-header" @click="item.showReasoning = !item.showReasoning">
	                    <span class="reasoning-icon">💭</span>
	                    <span class="reasoning-title">{{ item.isThinking ? '正在思考...' : '思考完成' }}</span>
	                    <span class="reasoning-toggle">{{ item.showReasoning ? '▼' : '▶' }}</span>
	                  </div>
	                  <div v-if="item.showReasoning" class="reasoning-content">
	                    <MarkdownRenderer :content="item.reasoning" :showCodeOnly="true"></MarkdownRenderer>
	                  </div>
	                </div>
	                <!-- 正式内容 -->
	                <div class="message-bubble" :class="item.role=='user' ? 'user-bubble' : 'ai-bubble'">
	                  <MarkdownRenderer :content="item.content" :showCodeOnly="true"></MarkdownRenderer>
	                  <span v-if="item.isStreaming && !item.isThinking" class="streaming-cursor">|</span>
	                </div>
	              </div>
	            </div>
	          </template>
	          <!-- 流式消息占位 -->
	          <div id="streaming-container" class="streaming-container"></div>
	          <!-- AI 思考加载动画 -->
	          <div v-if="isLoading && !hasStreamContent" class="ai-thinking-indicator">
	            <div class="message-wrapper assistant-message">
	              <div class="message-avatar">
	                <div class="avatar ai-avatar">AI</div>
	              </div>
	              <div class="message-content">
	                <div class="message-bubble ai-bubble thinking-bubble">
	                <div class="thinking-dots">
	                  <span class="dot"></span>
	                  <span class="dot"></span>
	                  <span class="dot"></span>
	                </div>
	                <span class="thinking-text">AI正在思考中...</span>
	              </div>
	              </div>
	            </div>
	          </div>
	        </div>
	        
	        <!-- 输入区域 -->
	        <div class="input-area product-input-area">
	          <div class="input-wrapper">
	            <div class="textarea-container">
	              <textarea
	                class="message-textarea"
	                v-model="message"
	                :placeholder="isLoading ? 'AI正在思考中...' : '输入你的问题...'"
	                @keydown.enter.exact.prevent="handleSubmit"
	                rows="2"
	                ref="textareaRef"
	                :disabled="isLoading"
	              />
	            </div>
	            <div class="input-actions">
                <el-space>
	              <el-select size="small" style="width:140px" v-model="search_model" placeholder="请选择模型">
	                <el-option label="通义千问-Turbo" value="qwen-turbo">通义千问-Turbo</el-option>
	                <el-option label="通义千问-Plus" value="qwen-plus">通义千问-Plus</el-option>
	                <el-option label="通义千问-Max" value="qwen-max">通义千问-Max</el-option>
	                <el-option label="deepseek-v3.2" value="deepseek-v3.2">DeepSeek-阿里云</el-option>
	                <el-option label="siliconflow/deepseek-v3.2" value="siliconflow/deepseek-v3.2">DeepSeek-硅基流动</el-option>
	              </el-select>
	              <el-button 
	                type="danger" 
	                class="clear-btn"
                  link
	                @click="handleClearHistory" 
	                :disabled="messages.length === 0"
	                title="清空历史消息"
	              >
	                <el-icon><Delete /></el-icon>
	              </el-button>
                </el-space>
	              <el-button 
	                type="primary" 
	                class="send-btn" 
	                @click="handleSubmit" 
	                :disabled="!message.trim() || isLoading"
	                :loading="isLoading"
	              >
	                <el-icon><Promotion /></el-icon>
	                <span>发送</span>
	              </el-button>
	            </div>
	          </div>
	        </div>
	     </el-col>
  </div>
</template>

<script setup>
import { ref,reactive, onMounted, toRefs,nextTick, watch } from 'vue';
import { useRoute } from 'vue-router';
import deepseekApi from '@/api/sys/tool/deepseekApi.js';
import { getDictItemByCodeAndName, listDictsByCode } from '@/api/sys/admin/dict.js';
import { getHelpDocByPath } from '@/api/sys/admin/helpDoc.js';
import MarkdownRenderer from "./components/MarkdownRenderer.vue"
import {Top,Plus,Promotion,Delete} from '@element-plus/icons-vue';
const emit = defineEmits(['change']);
const props = defineProps({
  innerType: {
    type: String,
    default: 'deepseek'
  },
  drawerMode: {
    type: Boolean,
    default: false
  }
});

const route = useRoute();
const { innerType, drawerMode } = props;

const textareaRef = ref(null)
const messagesWrapperRef=ref();

// 帮助文档库缓存（整个系统的所有帮助文档）
let helpDocLibrary = null;

    const  state=reactive({
		message:"",
		messages:[],
		sessionid:null,
		search_model:"deepseek-v3.2",
		search_network:"deepseek-chat",
		isLoading:false,
		sessions:[],
		searchKeys:[],
		response:null,
		streamEnabled: true, // 是否启用流式读取
		hasStreamContent: false, // 是否有流式内容
		agentMode: true, // 是否启用Agent模式（自动调用系统接口）
		toolCalls: [], // 当前正在执行的工具调用
	})
	const{
		message,
		messages,
		sessionid,
		searchKeys,
		sessions,
		search_model,
		search_network,
		isLoading,
		response,
		streamEnabled,
		hasStreamContent,
		agentMode,
		toolCalls,
	}=toRefs(state);
  function submit(data,callback){
    data.model=state.search_model;
    data.frequencyPenalty=0;
    data.maxTokens=4096;
    data.presencePenalty=0;
    data.responseFormat={"type":"json_object"};
    data.stop=null;
    data.stream=false;
    data.streamOptions=null;
    data.temperature=1;
    data.topP=1;
    data.sessionId=state.sessionid;
    data.tools=null;
    data.toolChoice="none";
    data.logprobs=false;
    data.topLogprobs=null;
    state.isLoading=true;
    deepseekApi.search(data).then(res=>{
      if(res.data){
        state.response=res.data;
        state.isLoading=false;
        // 从服务端响应中提取AI回复，追加到现有消息列表（不覆盖历史）
        if (state.response.messages && state.response.messages.length > 0) {
          const aiReply = state.response.messages[state.response.messages.length - 1];
          const aiMessage = {
            ...aiReply,
            reasoning: aiReply.reasoning_content || aiReply.reasoning || '',
            showReasoning: false,
            isStreaming: false,
            isThinking: false
          };
          state.messages = [...state.messages, aiMessage];
        }
        if (state.response.id) {
          state.sessionid = state.response.id;
        }
        emit("change");
        if(callback){
          callback(res.data);
        }
        nextTick(()=>{
          scrollToBottom();
        })
      }

    })
  }

  // 流式提交方法
  function submitStream(data, callback) {
    data.model = state.search_model;
    data.frequencyPenalty = 0;
    data.maxTokens = 4096;
    data.presencePenalty = 0;
    data.responseFormat = {"type": "json_object"};
    data.stop = null;
    data.stream = true;
    data.streamOptions = null;
    data.temperature = 1;
    data.topP = 1;
    data.sessionId = state.sessionid;
    data.tools = null;
    data.toolChoice = "none";
    data.logprobs = false;
    data.topLogprobs = null;
    state.isLoading = true;
    state.hasStreamContent = false; // 重置流式内容状态

    // 存储完整内容用于保存
    let fullReasoning = '';
    let fullContent = '';
    let hasReceivedData = false;
    
    // 获取流式消息容器
    const container = document.getElementById('streaming-container');
    if (!container) {
      console.error('找不到流式消息容器');
      return;
    }
    
    // 清空容器并创建流式消息元素
    container.innerHTML = `
      <div class="message-wrapper assistant-message" id="streaming-message">
        <div class="message-avatar">
          <div class="avatar ai-avatar">AI</div>
        </div>
        <div class="message-content">
          <div class="reasoning-block" id="streaming-reasoning" style="display:none;">
            <div class="reasoning-header">
              <span class="reasoning-icon">💭</span>
              <span class="reasoning-title">正在思考...</span>
            </div>
            <div class="reasoning-content" id="streaming-reasoning-content"></div>
          </div>
          <div class="message-bubble ai-bubble">
            <div id="streaming-content"></div>
            <span class="streaming-cursor">|</span>
          </div>
        </div>
      </div>
    `;

    deepseekApi.searchStream(
      data,
      // onMessage - 接收流式数据
      (parsed) => {
        // 第一次接收到数据时，触发 change 事件以关闭 loading
        if (!hasReceivedData) {
          hasReceivedData = true;
          state.hasStreamContent = true; // 标记有流式内容
          emit("change");
        }
        
        // 处理思考内容 - 直接操作 DOM
        if (parsed.reasoning) {
          fullReasoning += parsed.reasoning;
          const reasoningBlock = document.getElementById('streaming-reasoning');
          const reasoningContent = document.getElementById('streaming-reasoning-content');
          
          if (reasoningBlock && reasoningContent) {
            reasoningBlock.style.display = 'block';
            reasoningContent.innerHTML = fullReasoning.replace(/\n/g, '<br>');
          }
        }
        
        // 处理正式内容
        if (parsed.content && parsed.content !== '[DONE]') {
          fullContent += parsed.content;
          const contentDiv = document.getElementById('streaming-content');
          
          if (contentDiv) {
            contentDiv.innerHTML = fullContent.replace(/\n/g, '<br>');
          }
        }
        
        if (parsed.id) {
          state.sessionid = parsed.id;
        }
        
        // 滚动到底部
        scrollToBottom();
      },
      // onError - 错误处理
      (error) => {
        console.error('Stream error:', error);
        state.isLoading = false;
        // 移除流式消息元素
        const streamingMsg = document.getElementById('streaming-message');
        if (streamingMsg) {
          streamingMsg.remove();
        }
        // 出错时回退到普通请求
        deepseekApi.search(data).then(res => {
          if (res.data) {
            state.response = res.data;
            // 追加AI回复而非覆盖历史
            if (state.response.messages && state.response.messages.length > 0) {
              const aiReply = state.response.messages[state.response.messages.length - 1];
              state.messages = [...state.messages, {
                ...aiReply,
                reasoning: aiReply.reasoning_content || aiReply.reasoning || '',
                showReasoning: false,
                isStreaming: false,
                isThinking: false
              }];
            }
            if (state.response.id) {
              state.sessionid = state.response.id;
            }
            emit("change");
            nextTick(() => {
              scrollToBottom();
            });
          }
        });
      },
      // onComplete - 完成，接收sessionId
      (sessionId) => {
        state.isLoading = false;
        
        // 移除流式消息元素
        const streamingMsg = document.getElementById('streaming-message');
        if (streamingMsg) {
          streamingMsg.remove();
        }
        
        // 更新sessionId（如果是新会话）
        if (sessionId && !state.sessionid) {
          state.sessionid = sessionId;
        }
        
        // 将流式内容添加到消息列表
        const aiMessage = {
          id: Date.now(),
          message_id: Date.now(),
          role: 'assistant',
          content: fullContent,
          reasoning: fullReasoning,
          format_type: 'plain',
          message_type: 'text',
          created_time: new Date(),
          showReasoning: false,
          isStreaming: false,
          isThinking: false
        };
        state.messages = [...state.messages, aiMessage];
        
        emit("change");
        // 重新加载会话列表
        deepseekApi.getSession().then(res => {
          state.sessions = res.data;
        });
        if (callback) {
          callback(aiMessage);
        }
        nextTick(() => {
          scrollToBottom();
        });
      }
    );
  }

  // Agent模式流式提交方法（支持工具调用）
  function submitAgentStream(data, callback) {
    data.model = state.search_model;
    data.frequencyPenalty = 0;
    data.maxTokens = 4096;
    data.presencePenalty = 0;
    data.responseFormat = {"type": "json_object"};
    data.stop = null;
    data.stream = true;
    data.streamOptions = null;
    data.temperature = 1;
    data.topP = 1;
    data.sessionId = state.sessionid;
    data.agentMode = true;
    state.isLoading = true;
    state.hasStreamContent = false;
    state.toolCalls = []; // 清空工具调用列表

    let fullContent = '';
    let fullReasoning = '';
    let hasReceivedData = false;
    let agentSteps = []; // 执行步骤列表
    let currentIteration = 0; // 当前迭代轮次
    let panelCollapsed = false; // 面板是否折叠

    // 获取流式消息容器
    const container = document.getElementById('streaming-container');
    if (!container) {
      console.error('找不到流式消息容器');
      return;
    }

    // 清空容器并创建流式消息元素（含执行过程面板）
    container.innerHTML = `
      <div class="message-wrapper assistant-message" id="streaming-message">
        <div class="message-avatar">
          <div class="avatar ai-avatar">AI</div>
        </div>
        <div class="message-content">
          <!-- 执行过程面板 -->
          <div id="agent-process-panel" class="agent-process-panel">
            <div class="process-panel-header" id="process-panel-header">
              <span class="process-panel-icon">⚡</span>
              <span class="process-panel-title" id="process-panel-title">正在连接AI服务...</span>
              <span class="process-panel-toggle" id="process-panel-toggle">▼</span>
            </div>
            <div class="process-panel-body" id="process-panel-body">
              <div class="process-timeline" id="process-timeline">
                <div class="process-step step-active" id="step-init">
                  <div class="step-dot"></div>
                  <div class="step-content">
                    <span class="step-text">正在连接AI服务...</span>
                    <span class="step-time"></span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <!-- 最终回答区域 -->
          <div class="message-bubble ai-bubble" id="final-answer-bubble" style="display:none;">
            <div id="streaming-content"></div>
            <span class="streaming-cursor">|</span>
          </div>
        </div>
      </div>
    `;

    // 面板折叠/展开
    const panelHeader = document.getElementById('process-panel-header');
    if (panelHeader) {
      panelHeader.onclick = () => {
        panelCollapsed = !panelCollapsed;
        const body = document.getElementById('process-panel-body');
        const toggle = document.getElementById('process-panel-toggle');
        if (body) body.style.display = panelCollapsed ? 'none' : 'block';
        if (toggle) toggle.textContent = panelCollapsed ? '▶' : '▼';
      };
    }

    // 添加步骤到时间线
    function addStep(text, type) {
      const timeline = document.getElementById('process-timeline');
      if (!timeline) return;
      const now = new Date();
      const timeStr = now.getHours().toString().padStart(2,'0') + ':' + now.getMinutes().toString().padStart(2,'0') + ':' + now.getSeconds().toString().padStart(2,'0');
      const stepId = 'step-' + Date.now();
      const dotClass = type === 'error' ? 'step-dot-error' : type === 'success' ? 'step-dot-success' : type === 'tool' ? 'step-dot-tool' : 'step-dot-active';
      const html = `
        <div class="process-step step-${type || 'info'}" id="${stepId}">
          <div class="step-dot ${dotClass}"></div>
          <div class="step-content">
            <span class="step-text">${text}</span>
            <span class="step-time">${timeStr}</span>
          </div>
        </div>
      `;
      timeline.insertAdjacentHTML('beforeend', html);
      // 滚动面板到底部
      const body = document.getElementById('process-panel-body');
      if (body) body.scrollTop = body.scrollHeight;
      scrollToBottom();
    }

    // 更新面板标题
    function updatePanelTitle(title) {
      const el = document.getElementById('process-panel-title');
      if (el) el.textContent = title;
    }

    // 启动动态省略号动画
    let dotCount = 0;
    let thinkingTextBase = 'AI正在连接';
    const dotInterval = setInterval(() => {
      dotCount = (dotCount + 1) % 4;
      const dots = '.'.repeat(dotCount);
      const titleEl = document.getElementById('process-panel-title');
      if (titleEl) {
        titleEl.textContent = thinkingTextBase + dots;
      } else {
        clearInterval(dotInterval);
      }
    }, 400);

    deepseekApi.searchAgentStream(
      data,
      // onMessage - 接收流式数据
      (parsed) => {
        if (!hasReceivedData) {
          hasReceivedData = true;
          state.hasStreamContent = true;
          emit("change");
        }

        // 处理状态事件
        if (parsed.type === 'status') {
          const msg = parsed.message || '';
          thinkingTextBase = msg.replace(/\.{0,3}$/, '');
          const titleEl = document.getElementById('process-panel-title');
          if (titleEl) titleEl.textContent = thinkingTextBase + '.';
          dotCount = 1;

          // 根据状态添加步骤
          if (msg.includes('准备')) {
            addStep('正在准备系统提示词和工具定义...', 'info');
          } else if (msg.includes('选择工具')) {
            addStep('正在分析用户问题，选择合适的工具...', 'info');
          } else if (msg.includes('思考')) {
            addStep('AI正在思考中...', 'info');
          } else if (msg.includes('调用AI')) {
            addStep('正在调用AI模型...', 'info');
          }
          scrollToBottom();
          return;
        }

        // 处理AI调用详情事件（后端发送的详细执行信息）
        if (parsed.type === 'ai_calling') {
          addStep(parsed.message, 'info');
          updatePanelTitle(parsed.message);
          scrollToBottom();
          return;
        }

        if (parsed.type === 'ai_response') {
          addStep(parsed.message, parsed.message.includes('工具调用') ? 'tool' : 'success');
          scrollToBottom();
          return;
        }

        // 处理错误事件
        if (parsed.type === 'error') {
          clearInterval(dotInterval);
          const errMsg = parsed.message || 'AI调用失败';
          const errDetail = parsed.detail || '';
          addStep(`${errMsg}${errDetail ? ': ' + errDetail : ''}`, 'error');
          updatePanelTitle('执行出错');
          state.isLoading = false;
          return;
        }

        // 处理工具调用事件
        if (parsed.type === 'tool_call') {
          currentIteration++;
          const toolCall = {
            id: parsed.tool_call_id,
            functionName: parsed.function_name,
            description: parsed.function_description,
            endpoint: parsed.endpoint,
            method: parsed.method,
            service: parsed.service,
            module: parsed.module,
            arguments: parsed.arguments,
            status: 'calling',
            result: null
          };
          state.toolCalls.push(toolCall);
          updatePanelTitle(`正在执行工具调用 #${currentIteration}`);

          // 添加工具调用步骤
          const methodBadge = parsed.method ? `[${parsed.method}]` : '';
          const endpointInfo = parsed.endpoint ? ` ${parsed.endpoint}` : '';
          addStep(`调用工具: ${parsed.function_description || parsed.function_name}${methodBadge}${endpointInfo}`, 'tool');

          // 显示请求参数
          if (parsed.arguments) {
            try {
              const argsStr = JSON.stringify(JSON.parse(parsed.arguments), null, 2);
              addStep(`请求参数: ${argsStr}`, 'params');
            } catch(e) {
              addStep(`请求参数: ${parsed.arguments}`, 'params');
            }
          }

          renderToolCalls();
          scrollToBottom();
          return;
        }

        // 处理工具结果事件
        if (parsed.type === 'tool_result') {
          const toolCall = state.toolCalls.find(t => t.id === parsed.tool_call_id);
          if (toolCall) {
            toolCall.status = 'completed';
            toolCall.result = parsed.result;
            renderToolCalls();

            // 添加结果步骤
            const resultPreview = formatToolResult(parsed.result);
            const shortResult = resultPreview.length > 200 ? resultPreview.substring(0, 200) + '...' : resultPreview;
            addStep(`返回结果: ${shortResult}`, 'success');
          }
          scrollToBottom();
          return;
        }

        // 处理推理/思考过程（显示在面板中，折叠形式）
        if (parsed.reasoning) {
          fullReasoning += parsed.reasoning;
          // 只在第一次收到思考内容时添加一个可折叠的步骤
          const existingThinking = document.getElementById('step-thinking-block');
          if (!existingThinking) {
            const timeline = document.getElementById('process-timeline');
            if (timeline) {
              const now = new Date();
              const timeStr = now.getHours().toString().padStart(2,'0') + ':' + now.getMinutes().toString().padStart(2,'0') + ':' + now.getSeconds().toString().padStart(2,'0');
              const html = `
                <div class="process-step step-thinking" id="step-thinking-block">
                  <div class="step-dot step-dot-active"></div>
                  <div class="step-content">
                    <div class="thinking-block-header" onclick="this.parentElement.querySelector('.thinking-block-body').style.display = this.parentElement.querySelector('.thinking-block-body').style.display === 'none' ? 'block' : 'none'; this.querySelector('.thinking-toggle').textContent = this.parentElement.querySelector('.thinking-block-body').style.display === 'none' ? '▶' : '▼';">
                      <span class="step-text" style="cursor:pointer;">AI思考过程 (点击展开)</span>
                      <span class="thinking-toggle" style="font-size:11px;color:#909399;margin-left:4px;">▶</span>
                      <span class="step-time">${timeStr}</span>
                    </div>
                    <div class="thinking-block-body" style="display:none;">
                      <pre class="thinking-detail" id="thinking-detail-text"></pre>
                    </div>
                  </div>
                </div>
              `;
              timeline.insertAdjacentHTML('beforeend', html);
            }
          }
          // 追加思考内容
          const detailEl = document.getElementById('thinking-detail-text');
          if (detailEl) {
            detailEl.textContent += parsed.reasoning;
            detailEl.scrollTop = detailEl.scrollHeight;
          }
          scrollToBottom();
          return;
        }

        // 处理正式内容
        if (parsed.content && parsed.content !== '[DONE]') {
          fullContent += parsed.content;

          // 第一次收到内容时，隐藏过程面板并显示回答区域
          if (fullContent.length === parsed.content.length) {
            const bubble = document.getElementById('final-answer-bubble');
            if (bubble) bubble.style.display = 'block';
            // 自动折叠过程面板
            const body = document.getElementById('process-panel-body');
            const toggle = document.getElementById('process-panel-toggle');
            if (body) body.style.display = 'none';
            if (toggle) toggle.textContent = '▶';
            updatePanelTitle(`执行完成 (${currentIteration}次工具调用)`);
            addStep('正在生成最终答案...', 'success');
          }

          const contentDiv = document.getElementById('streaming-content');
          if (contentDiv) {
            contentDiv.innerHTML = fullContent.replace(/\n/g, '<br>');
          }
          scrollToBottom();
        }

        if (parsed.id) {
          state.sessionid = parsed.id;
        }
      },
      // onError - 错误处理
      (error) => {
        console.error('Agent Stream error:', error);
        clearInterval(dotInterval);
        state.isLoading = false;
        state.toolCalls = [];
        const streamingMsg = document.getElementById('streaming-message');
        if (streamingMsg) {
          streamingMsg.remove();
        }
        const errorMessage = {
          id: Date.now(),
          message_id: Date.now(),
          role: 'assistant',
          content: '抱歉，AI Agent处理请求时发生错误，请重试。',
          format_type: 'plain',
          message_type: 'text',
          created_time: new Date(),
          showReasoning: false,
          isStreaming: false,
          isThinking: false
        };
        state.messages = [...state.messages, errorMessage];
        nextTick(() => { scrollToBottom(); });
      },
      // onComplete - 完成
      (sessionId) => {
        clearInterval(dotInterval);
        state.isLoading = false;

        const streamingMsg = document.getElementById('streaming-message');
        if (streamingMsg) {
          streamingMsg.remove();
        }

        if (sessionId && !state.sessionid) {
          state.sessionid = sessionId;
        }

        // 将流式内容添加到消息列表（含执行过程）
        const aiMessage = {
          id: Date.now(),
          message_id: Date.now(),
          role: 'assistant',
          content: fullContent,
          reasoning: fullReasoning || '',
          format_type: 'plain',
          message_type: 'text',
          created_time: new Date(),
          showReasoning: false,
          isStreaming: false,
          isThinking: false,
          // 保存执行过程数据
          agentSteps: agentSteps.length > 0 ? agentSteps : null,
          toolCalls: state.toolCalls.length > 0 ? [...state.toolCalls] : null
        };
        state.messages = [...state.messages, aiMessage];
        state.toolCalls = [];

        emit("change");
        deepseekApi.getSession().then(res => {
          state.sessions = res.data;
        });
        if (callback) {
          callback(aiMessage);
        }
        nextTick(() => { scrollToBottom(); });
      }
    );
  }

  // 渲染工具调用列表（折叠卡片）
  function renderToolCalls() {
    // 工具调用已在addStep中实时渲染到时间线，这里不需要额外处理
  }

  // HTML转义
  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  // 格式化工具结果 - 展示实际数据
  function formatToolResult(result) {
    try {
      const parsed = JSON.parse(result);
      if (parsed.success) {
        // 展示实际返回数据，而非只显示"查询成功"
        if (parsed.data) {
          const data = parsed.data;
          // 如果是分页数据，展示records
          if (data.records && Array.isArray(data.records)) {
            const count = data.total || data.records.length;
            const preview = data.records.slice(0, 3);
            let previewJson = JSON.stringify(preview, null, 2);
            if (data.records.length > 3) {
              previewJson = previewJson.replace(/\]$/, ',\n  ...(还有' + (data.records.length - 3) + '条)');
            }
            return '共' + count + '条记录\n' + previewJson;
          }
          // 如果data是数组
          if (Array.isArray(data)) {
            const preview = data.slice(0, 3);
            let previewJson = JSON.stringify(preview, null, 2);
            if (data.length > 3) {
              previewJson = previewJson.replace(/\]$/, ',\n  ...(还有' + (data.length - 3) + '条)');
            }
            return '共' + data.length + '条\n' + previewJson;
          }
          // 其他情况展示完整data
          const dataStr = JSON.stringify(data, null, 2);
          if (dataStr.length > 1000) {
            return dataStr.substring(0, 1000) + '\n...(已截断)';
          }
          return dataStr;
        }
        return parsed.message || '查询成功（无数据）';
      } else {
        return parsed.error || parsed.message || '查询失败';
      }
    } catch (e) {
      if (result.length > 500) {
        return result.substring(0, 500) + '\n...(已截断)';
      }
      return result;
    }
  }

	function handleSubmit(){
		var data={};
		if(state.message) {
			// 构建用户消息对象
			const userMessage = {
				message_id: Date.now(),
				role: 'user',
				content: state.message,
				format_type: 'plain',
				message_type: 'text',
				created_time: new Date()
			};

			// 追加用户消息到现有历史（保留之前的对话）
			state.messages = [...state.messages, userMessage];

			// 清空输入框
			state.message = "";

			// 清空流式消息容器
			const container = document.getElementById('streaming-container');
			if (container) {
				container.innerHTML = '';
			}

			// 滚动到底部
			nextTick(() => {
				scrollToBottom();
			});

			// 发送完整的历史消息给后端（保持上下文连贯）
			data.messages = state.messages.map(msg => ({
				"role": msg.role,
				"content": msg.content
			}));

			// 获取当前页面路径和页面标题
			const currentPath = window.location.hash ? window.location.hash.substring(1) : window.location.pathname;
			data.currentPage = currentPath;
			
			// 获取当前路由的页面标题（用于查询帮助文档）
			const pageTitle = route.meta?.title || '';
			data.pageTitle = pageTitle;

			// 加载帮助文档库和当前页面的帮助文档内容
			Promise.all([
				loadHelpDocLibrary(),
				loadCurrentPageHelpDoc(currentPath)
			]).then(([helpDocs, currentHelpDoc]) => {
				if (helpDocs) {
					// 传递整个帮助文档库（转换为JSON字符串）
					data.helpDocLibrary = JSON.stringify(helpDocs);
					// 同时传递当前页面的帮助文档URL
					const currentHelp = helpDocs.find(d => d.name === pageTitle);
					if (currentHelp && currentHelp.value) {
						data.helpDocUrl = currentHelp.value;
					}
				}
				
				// 传递当前页面的帮助文档内容
				if (currentHelpDoc) {
					data.currentHelpDoc = JSON.stringify(currentHelpDoc);
				}
				
				// 根据模式选择提交方式
				if (agentMode.value) {
					// Agent模式：自动调用系统接口
					submitAgentStream(data);
				} else if (streamEnabled.value) {
					// 普通流式模式
					submitStream(data);
				} else {
					// 同步模式
					submit(data);
				}
			});
		}
	}
	
	// 加载整个帮助文档库（带缓存）
	async function loadHelpDocLibrary() {
		// 如果已缓存，直接返回
		if (helpDocLibrary !== null) {
			return helpDocLibrary;
		}
		try {
			const res = await listDictsByCode('helppage');
			if (res.data && Array.isArray(res.data)) {
				// 只保留name和value字段，减少数据量
				helpDocLibrary = res.data
					.filter(d => d.name && d.value)
					.map(d => ({
						name: d.name,
						url: d.value
					}));
				return helpDocLibrary;
			}
		} catch (e) {
			console.warn('获取帮助文档库失败:', e);
		}
		return null;
	}
	
	// 加载当前页面的帮助文档内容
	async function loadCurrentPageHelpDoc(path) {
		try {
			const res = await getHelpDocByPath(path);
			if (res.data) {
				return {
					docKey: res.data.docKey,
					title: res.data.title,
					content: res.data.content,
					category: res.data.category
				};
			}
		} catch (e) {
			console.warn('获取当前页面帮助文档失败:', e);
		}
		return null;
	}
 function handleSession(session){
	 // 加载历史消息时，添加 reasoning 相关字段
	 state.messages = session.messages.map(msg => ({
		 ...msg,
		 reasoning: msg.reasoning_content || msg.reasoning || '',
		 showReasoning: false,
		 isStreaming: false,
		 isThinking: false
	 }));
	 state.sessionid=session.id;
	 nextTick(()=>{
		 scrollToBottom();
	 })
	 
 }
 function handleContent(content){
	 state.message=state.message+" "+content;
 }
 function handleAddSession(){
	 state.messages=messages.value = [
										{
										  message_id: 1,
										  role: 'assistant',
										  content: '你好！我是AI助手，我可以帮助你解答问题。请问有什么可以帮您的？',
										  format_type: 'plain',
										  message_type: 'text',
										  created_time: new Date()
										}
									  ];
	 state.sessionid=null;
 }
 
 // 清空历史消息
 function handleClearHistory(){
   // 删除当前会话（如果有的话）
   const currentSessionId = state.sessionid;
   if (currentSessionId) {
     deepseekApi.deleteSession(currentSessionId).catch(err => {
       console.error('删除会话失败:', err);
     });
   }
   
   state.messages = [];
   state.sessionid = null;
   // 清空流式消息容器
   const container = document.getElementById('streaming-container');
   if (container) {
     container.innerHTML = '';
   }
 }
 
 // 删除单个会话
 function handleDeleteSession(session){
   deepseekApi.deleteSession(session.id).then(() => {
     // 从列表中移除
     state.sessions = state.sessions.filter(s => s.id !== session.id);
     // 如果删除的是当前会话，清空消息
     if (state.sessionid === session.id) {
       state.messages = [];
       state.sessionid = null;
       const container = document.getElementById('streaming-container');
       if (container) {
         container.innerHTML = '';
       }
     }
   }).catch(err => {
     console.error('删除会话失败:', err);
   });
 }
 
function scrollToBottom(){
	if (messagesWrapperRef.value) {
		messagesWrapperRef.value.scrollTop = messagesWrapperRef.value.scrollHeight;
	}
}

// 自动调整文本框高度
watch(state.messages, () => {
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
      textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 120) + 'px'
    }
  })
})


// 初始化示例消息
onMounted(async () => {
  // 获取当前页面路径
  const currentPath = window.location.hash ? window.location.hash.substring(1) : window.location.pathname;
  const pageTitle = route.meta?.title || '';
  console.log('[AI助手] 当前页面路径:', currentPath, '页面标题:', pageTitle);
  
  // 尝试获取当前页面的帮助文档
  let helpDocContent = null;
  try {
    const res = await getHelpDocByPath(currentPath);
    console.log('[AI助手] 帮助文档API响应:', res);
    if (res.data && res.data.content) {
      helpDocContent = res.data.content;
      console.log('[AI助手] 获取到帮助文档内容');
    }
  } catch (e) {
    console.warn('[AI助手] 获取帮助文档失败:', e);
  }
  
  // 根据是否是抽屉模式设置欢迎消息
  let welcomeContent = '你好！我是AI助手，我可以帮助你查询业务数据和解答问题。请问有什么可以帮您的？';
  
  if (drawerMode) {
    if (helpDocContent) {
      // 有帮助文档时，显示详细内容
      welcomeContent = `**当前页面：${pageTitle || currentPath}**\n\n${helpDocContent}`;
    } else {
      // 没有帮助文档时，根据路径生成简要介绍
      const pathParts = currentPath.split('/').filter(p => p);
      const moduleMap = {
        'amazon': 'Amazon平台',
        'erp': 'ERP系统',
        'finance': '财务模块',
        'sys': '系统设置',
        'purchase': '采购管理',
        'inventory': '库存管理',
        'ship': '发货管理',
        'warehouse': '仓库管理',
        'order': '订单管理',
        'profit': '利润分析',
        'report': '报表',
        'sale': '销售',
        'advertising': '广告管理'
      };
      
      // 尝试从路径中提取模块信息
      let moduleDesc = '';
      for (const part of pathParts) {
        if (moduleMap[part]) {
          moduleDesc += moduleMap[part] + ' > ';
        }
      }
      if (moduleDesc.endsWith(' > ')) {
        moduleDesc = moduleDesc.slice(0, -3);
      }
      
      welcomeContent = `**当前页面：${pageTitle || moduleDesc || currentPath}**\n\n` +
        `欢迎使用AI智能助手！我可以帮助您：\n\n` +
        `- 查询业务数据（库存、订单、采购等）\n` +
        `- 解答系统操作问题\n` +
        `- 分析销售趋势和数据\n\n` +
        `请问有什么可以帮您的？`;
    }
  }
  
  state.messages = [
    {
      message_id: 1,
      role: 'assistant',
      content: welcomeContent,
      format_type: 'markdown',
      message_type: 'text',
      created_time: new Date()
    }
  ]
  deepseekApi.getSession().then(res=>{
	  state.sessions=res.data;
  });
  deepseekApi.getKey().then(res=>{
  	  state.searchKeys=res.data;
  });


  
})
 function show(dataRow,ftype,callback){
   // 默认使用流式模式
   state.streamEnabled = true;
   
   if(ftype=="addMsg"){
     var data={};
		var messages=[];
     state.message=dataRow;
		if(state.message) {
			messages.push({"role":"system","content":state.message});
       data.messages=messages;
       // 使用流式提交
       submitStream(data,callback);
     }
   }else if(ftype=="initMsg"){
     // 获取会话列表，查找匹配的会话
     deepseekApi.getSession().then(res=>{
       state.sessions=res.data;
       
       // 查找标题匹配且消息数不超过30轮的会话
       const targetTitle = dataRow.title;
       const existingSession = state.sessions.find(session => {
         if (session.title !== targetTitle) return false;
         // 检查消息数量（每轮包含用户消息和AI回复）
         const messageCount = session.messages ? session.messages.length : 0;
         return messageCount < 60; // 30轮 = 60条消息
       });
       
       if (existingSession) {
         // 使用现有会话
         state.sessionid = existingSession.id;
         
         // 将当前信息作为新的用户消息
         const userMessage = {
           message_id: Date.now(),
           role: 'user',
           content: dataRow.content,
           format_type: 'plain',
           message_type: 'text',
           created_time: new Date()
         };
         
         // 一次性更新消息列表，减少重渲染
         state.messages = [
           ...existingSession.messages.map(msg => ({
             ...msg,
             role: msg.role === 'system' ? 'user' : msg.role,
             reasoning: msg.reasoning_content || msg.reasoning || '',
             showReasoning: false,
             isStreaming: false,
             isThinking: false
           })),
           userMessage
         ];
         
         nextTick(() => {
           scrollToBottom();
         });
         
         // 发送给AI继续对话
         var data={};
         var messages=[];
         // 添加历史消息到请求中（使用原始角色）
         existingSession.messages.forEach(msg => {
           messages.push({"role": msg.role, "content": msg.content});
         });
         // 添加当前用户消息
         messages.push({"role":"user","content": dataRow.content});
         data.messages=messages;
         data.sessionId = existingSession.id;
         
         // 使用流式提交
         submitStream(data,callback);
       } else {
         // 新建会话
         state.sessionid=null;
         // 显示系统消息
         const systemMessage = {
           message_id: Date.now(),
           role: 'user',
           content: dataRow.content,
           format_type: 'plain',
           message_type: 'text',
           created_time: new Date()
         };
         state.messages = [systemMessage];
         
         nextTick(() => {
           scrollToBottom();
         });
         
         var data={};
         var messages=[];
         messages.push({"role":"system","content":dataRow.title});
         messages.push({"role":"system","content":dataRow.content});
         data.messages=messages;
         // 使用流式提交
         submitStream(data,callback);
       }
     });
   }
 }
defineExpose({
  show,
})

</script>

<style scoped>
.deepseek-app {
  height: calc(100vh - 40px);
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  overflow: hidden;
}

.deepseek-app.drawer-mode {
  height: 100%;
  background: none;
}

.deepseek-app.drawer-mode :deep(.deepseek-row) {
  height: 100%;
}

.deepseek-app.drawer-mode :deep(.chat-main) {
  height: 100%;
  display: flex;
  background:none;
  flex-direction: column;
}

.deepseek-app.drawer-mode :deep(.messages-wrapper) {
  flex: 1;
  height: auto;
  overflow-y: auto;
  padding: 16px 24px;
  min-height: 0;
  border-radius: 16px;
  margin: 12px 24px;
}

.deepseek-app.drawer-mode :deep(.input-area) {
  flex-shrink: 0;
  padding: 12px 24px;
}

.deepseek-row {
  height: 100%;
}

/* 左侧边栏 */
.sidebar-left {
  background: #ffffff;
  border-right: 1px solid #e8eaec;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.04);
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #f0f2f5;
  background: #fafbfc;
}

.new-session-btn {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.new-session-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.session-scrollbar {
  flex: 1;
}

.session-list {
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin: 4px 0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #f8f9fb;
}

.session-item:hover {
  background: #eef0f3;
  transform: translateX(4px);
}

.session-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.session-item-content {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-item.active .session-title {
  color: #ffffff;
  font-weight: 600;
}

.session-item-actions {
  margin-left: 8px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.session-item:hover .session-item-actions {
  opacity: 1;
}

.session-item.active .session-item-actions .el-button {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.session-item.active .session-item-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 中间聊天区域 */
.chat-main {
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.messages-scrollbar {
  flex: 1;
}

.message-wrapper {
  display: flex;
  gap: 12px;
  animation: fadeIn 0.3s ease;
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.user-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.ai-avatar {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.message-content {
  max-width: 70%;
}

.message-bubble {
  padding: 16px 20px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
  word-wrap: break-word;
}

.user-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.ai-bubble {
  background: #ffffff;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* 输入区域 */
.input-area {
  padding: 16px 24px;
  background: #ffffff;
  border-top: 1px solid #e8eaec;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.04);
}

.input-wrapper {
  background: #f8f9fb;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid #e8eaec;
  transition: border-color 0.2s;
}

.input-wrapper:focus-within {
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.textarea-container {
  padding: 12px 16px 0;
}

.message-textarea {
  width: 100%;
  border: none;
  background: transparent;
  resize: none;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  min-height: 60px;
  max-height: 120px;
  outline: none;
  color: #303133;
}

.message-textarea::placeholder {
  color: #909399;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
}

.model-select {
  flex: 1;
}

.model-dropdown {
  width: 200px;
}

.model-dropdown :deep(.el-input__wrapper) {
  border-radius: 6px;
  background: #ffffff;
}

.send-btn {
  height: 40px;
  padding: 0 24px;
  border-radius: 8px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 右侧边栏 */
.sidebar-right {
  background: #ffffff;
  border-left: 1px solid #e8eaec;
  display: flex;
  flex-direction: column;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.04);
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.quick-phrases {
  padding: 12px;
}

.phrase-item {
  padding: 12px 16px;
  margin: 6px 0;
  background: #f8f9fb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.phrase-item:hover {
  background: #eef0f3;
  transform: translateX(-4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.phrase-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.empty-subtitle {
  font-size: 13px;
  color: #909399;
}

/* 动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-wrapper:nth-child(1) { animation-delay: 0.05s; }
.message-wrapper:nth-child(2) { animation-delay: 0.1s; }
.message-wrapper:nth-child(3) { animation-delay: 0.15s; }
.message-wrapper:nth-child(4) { animation-delay: 0.2s; }
.message-wrapper:nth-child(5) { animation-delay: 0.25s; }

/* 流式光标效果 */
.streaming-cursor {
  display: inline-block;
  color: #667eea;
  font-weight: bold;
  animation: cursorBlink 0.8s infinite;
  margin-left: 2px;
}

@keyframes cursorBlink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* 思考过程样式 */
.reasoning-block {
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
  background: #f8f9fb;
  border: 1px solid #e8eaec;
}

.reasoning-header {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s;
}

.reasoning-header:hover {
  background: #eef0f3;
}

.reasoning-icon {
  margin-right: 8px;
  font-size: 16px;
}

.reasoning-title {
  flex: 1;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.reasoning-toggle {
  font-size: 12px;
  color: #909399;
  transition: transform 0.2s;
}

.reasoning-content {
  padding: 12px 14px;
  border-top: 1px solid #e8eaec;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
  max-height: 300px;
  overflow-y: auto;
}

.reasoning-content :deep(p) {
  margin-bottom: 8px;
}

.reasoning-content :deep(p:last-child) {
  margin-bottom: 0;
}

/* 思考中动画 */
@keyframes thinkingPulse {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}

.reasoning-header:has(.reasoning-title:contains('正在思考')) .reasoning-icon {
  animation: thinkingPulse 1.5s infinite;
}

/* 消息区域样式 */
.messages-wrapper {
  height: calc(100vh - 220px);
  overflow-y: auto;
  padding: 24px;
}

.product-messages-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px 8px 0 0;
  border: 1px solid #e4e7ed;
  border-bottom: none;
}

.product-input-area {
  padding: 12px 16px;
  background: #fff;
  border-radius: 0 0 8px 8px;
  border: 1px solid #e4e7ed;
  border-top: 1px solid #f0f2f5;
}

.product-input-area .input-wrapper {
  border-radius: 8px;
}

.product-input-area .send-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: 6px;
}

.message-wrapper {
  margin-bottom: 20px;
}

.streaming-container {
  min-height: 50px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 16px;
}
.streaming-container:empty {
  display: none;
}
.stream-switch {
  margin-left: 12px;
}

.stream-switch :deep(.el-switch__core) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stream-switch :deep(.el-switch.is-checked .el-switch__core) {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

/* AI 思考加载动画 */
.ai-thinking-indicator {
  margin-bottom: 20px;
}

.thinking-bubble {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
}

.thinking-dots {
  display: flex;
  gap: 6px;
}

.thinking-dots .dot {
  width: 10px;
  height: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  animation: dotPulse 1.4s infinite ease-in-out both;
}

.thinking-dots .dot:nth-child(1) {
  animation-delay: -0.32s;
}

.thinking-dots .dot:nth-child(2) {
  animation-delay: -0.16s;
}

.thinking-dots .dot:nth-child(3) {
  animation-delay: 0s;
}

@keyframes dotPulse {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.thinking-text-animated {
  color: #667eea;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .sidebar-left,
  .sidebar-right {
    display: none;
  }

  .chat-main {
    width: 100%;
  }
}

/* Agent模式样式 */
.agent-switch {
  margin-left: 12px;
}

.agent-switch :deep(.el-switch__core) {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.agent-switch :deep(.el-switch.is-checked .el-switch__core) {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.agent-switch :deep(.el-switch__label) {
  font-size: 11px;
}

/* 执行过程面板 */
.agent-process-panel {
  margin-bottom: 12px;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  animation: fadeIn 0.3s ease;
}

.process-panel-header {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef0f3 100%);
  border-bottom: 1px solid #e4e7ed;
  transition: background 0.2s;
}

.process-panel-header:hover {
  background: linear-gradient(135deg, #eef0f3 0%, #e4e8ec 100%);
}

.process-panel-icon {
  margin-right: 8px;
  font-size: 14px;
}

.process-panel-title {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.process-panel-toggle {
  font-size: 12px;
  color: #909399;
  transition: transform 0.2s;
}

.process-panel-body {
  max-height: 300px;
  overflow-y: auto;
  padding: 8px 0;
}

/* 时间线样式 */
.process-timeline {
  padding: 0 14px;
}

.process-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 4px 0;
  position: relative;
}

.process-step:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 18px;
  bottom: -4px;
  width: 1px;
  background: #e4e7ed;
}

.step-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 3px;
  background: #c0c4cc;
  border: 2px solid #ffffff;
  box-shadow: 0 0 0 1px #c0c4cc;
}

.step-dot-active {
  background: #409eff;
  box-shadow: 0 0 0 1px #409eff;
  animation: dotPulse 1.4s infinite ease-in-out both;
}

.step-dot-tool {
  background: #e6a23c;
  box-shadow: 0 0 0 1px #e6a23c;
}

.step-dot-success {
  background: #67c23a;
  box-shadow: 0 0 0 1px #67c23a;
}

.step-dot-error {
  background: #f56c6c;
  box-shadow: 0 0 0 1px #f56c6c;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-text {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  word-break: break-all;
  white-space: pre-wrap;
}

.step-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-left: 8px;
  white-space: nowrap;
}

.step-error .step-text {
  color: #f56c6c;
  font-weight: 500;
}

.step-tool .step-text {
  color: #e6a23c;
  font-weight: 500;
}

.step-success .step-text {
  color: #67c23a;
}

.step-thinking .step-text {
  color: #909399;
  font-style: italic;
}

.step-params .step-text {
  color: #909399;
  font-size: 11px;
  font-family: monospace;
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
  display: block;
  max-height: 150px;
  overflow-y: auto;
}

/* 思考过程折叠块 */
.thinking-block-header {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  user-select: none;
}

.thinking-block-header:hover .step-text {
  color: #409eff;
}

.thinking-detail {
  background: #f8f9fb;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 11px;
  line-height: 1.6;
  color: #606266;
  margin: 4px 0 0 0;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: monospace;
}
</style>

<!-- 暗黑模式样式 - 非scoped确保生效 -->
<style>
/* 暗黑模式 - 纯黑主题 */
html.dark .deepseek-app {
  background: #0d0d0d !important;
}

html.dark .deepseek-app .sidebar-left,
html.dark .deepseek-app .sidebar-right {
  background: #111111 !important;
  border-color: #1a1a1a !important;
}

html.dark .deepseek-app .sidebar-header {
  background: #0d0d0d !important;
  border-color: #1a1a1a !important;
}

html.dark .deepseek-app .session-item {
  background: #161616 !important;
}

html.dark .deepseek-app .session-item:hover {
  background: #1f1f1f !important;
}

html.dark .deepseek-app .session-title {
  color: #e0e0e0 !important;
}

html.dark .deepseek-app .session-item.active .session-title {
  color: #ffffff !important;
}

html.dark .deepseek-app .sidebar-title {
  color: #e0e0e0 !important;
}

html.dark .deepseek-app .phrase-item {
  background: #161616 !important;
}

html.dark .deepseek-app .phrase-item:hover {
  background: #1f1f1f !important;
}

html.dark .deepseek-app .phrase-text {
  color: #c0c0c0 !important;
}

html.dark .deepseek-app .chat-main {
  background: #0d0d0d !important;
}

html.dark .deepseek-app .messages-wrapper {
  background: transparent !important;
}

html.dark .deepseek-app .ai-bubble {
  background: #161616 !important;
  color: #e0e0e0 !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3) !important;
}

html.dark .deepseek-app .input-area {
  background: #111111 !important;
  border-color: #1a1a1a !important;
}

html.dark .deepseek-app .input-wrapper {
  background: #161616 !important;
  border-color: #1a1a1a !important;
}

html.dark .deepseek-app .input-wrapper:focus-within {
  border-color: #667eea !important;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.2) !important;
}

html.dark .deepseek-app .message-textarea {
  color: #e0e0e0 !important;
}

html.dark .deepseek-app .message-textarea::placeholder {
  color: #666666 !important;
}

html.dark .deepseek-app .model-dropdown .el-input__wrapper {
  background: #111111 !important;
}

html.dark .deepseek-app .reasoning-block {
  background: #111111 !important;
  border-color: #1a1a1a !important;
}

html.dark .deepseek-app .reasoning-header:hover {
  background: #1f1f1f !important;
}

html.dark .deepseek-app .reasoning-title {
  color: #c0c0c0 !important;
}

html.dark .deepseek-app .reasoning-content {
  border-color: #1a1a1a !important;
  color: #a0a0a0 !important;
}

html.dark .deepseek-app .empty-icon {
  opacity: 0.6 !important;
}

html.dark .deepseek-app .empty-text {
  color: #e0e0e0 !important;
}

html.dark .deepseek-app .empty-subtitle {
  color: #666666 !important;
}

html.dark .deepseek-app .thinking-text {
  color: #666666 !important;
}

html.dark .deepseek-app .product-messages-wrapper {
  background: #0d0d0d !important;
  border-color: #1a1a1a !important;
}

html.dark .deepseek-app .product-input-area {
  background: #111111 !important;
  border-color: #1a1a1a !important;
}

html.dark .deepseek-app .streaming-container {
  background: #161616 !important;
}

/* AI头像在暗黑模式下的颜色 */
html.dark .deepseek-app .ai-avatar {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%) !important;
}

/* 思考动画圆点在暗黑模式下 */
html.dark .deepseek-app .thinking-dots .dot {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
}

/* 流式开关在暗黑模式下 */
html.dark .deepseek-app .stream-switch .el-switch__core {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
}

html.dark .deepseek-app .stream-switch.el-switch.is-checked .el-switch__core {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%) !important;
}

/* Agent开关在暗黑模式下 */
html.dark .deepseek-app .agent-switch .el-switch__core {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%) !important;
}

html.dark .deepseek-app .agent-switch.el-switch.is-checked .el-switch__core {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%) !important;
}

/* 执行过程面板在暗黑模式下 */
html.dark .deepseek-app .agent-process-panel {
  background: #161616 !important;
  border-color: #2d2d44 !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
}

html.dark .deepseek-app .process-panel-header {
  background: linear-gradient(135deg, #1a1a1a 0%, #1f1f1f 100%) !important;
  border-color: #2d2d44 !important;
}

html.dark .deepseek-app .process-panel-header:hover {
  background: linear-gradient(135deg, #1f1f1f 0%, #2d2d2d 100%) !important;
}

html.dark .deepseek-app .process-panel-title {
  color: #e0e0e0 !important;
}

html.dark .deepseek-app .step-text {
  color: #c0c0c0 !important;
}

html.dark .deepseek-app .step-time {
  color: #555 !important;
}

html.dark .deepseek-app .process-step:not(:last-child)::after {
  background: #2d2d44 !important;
}

html.dark .deepseek-app .step-dot {
  border-color: #161616 !important;
}

html.dark .deepseek-app .step-params .step-text {
  background: #1a1a1a !important;
  color: #808080 !important;
}

html.dark .deepseek-app .step-error .step-text {
  color: #f56c6c !important;
}

html.dark .deepseek-app .step-tool .step-text {
  color: #e6a23c !important;
}

html.dark .deepseek-app .step-success .step-text {
  color: #67c23a !important;
}

html.dark .deepseek-app .thinking-block-header:hover .step-text {
  color: #409eff !important;
}

html.dark .deepseek-app .thinking-detail {
  background: #1a1a1a !important;
  border-color: #2d2d44 !important;
  color: #a0a0a0 !important;
}

html.dark .deepseek-app .reasoning-text {
  color: #808080 !important;
  background: #1a1a1a !important;
}
</style>
/**
 * v-debounce-input 指令
 * 用于对input事件进行防抖处理
 * 
 * 使用方式：
 * <el-input v-debounce-input="handler" />
 * <el-input v-debounce-input:500="handler" />  // 自定义延迟时间
 */

export const debounceInput = {
    mounted(el, binding) {
        // 获取延迟时间，默认300ms
        const delay = binding.arg ? parseInt(binding.arg) : 300;
        
        // 获取处理函数
        const handler = binding.value;
        
        // 如果没有提供处理函数，直接返回
        if (typeof handler !== 'function') {
            console.warn('v-debounce-input 指令需要一个函数作为值');
            return;
        }
        
        // 防抖定时器
        let timeout = null;
        
        // 防抖处理函数
        const debouncedHandler = (value) => {
            // 清除之前的定时器
            if (timeout) {
                clearTimeout(timeout);
            }

            // 设置新的定时器
            timeout = setTimeout(() => {
                handler(value);
                timeout = null;
            }, delay);
        };
        
        // input事件处理函数
        const inputHandler = (e) => {
            debouncedHandler(e.target.value);
        };
        
        // 将处理函数存储在元素上，便于后续清理
        el._debounceInputHandler = inputHandler;
        
        // 添加事件监听
        el.addEventListener('input', inputHandler);
    },
    
    updated(el, binding) {
        // 如果值发生变化，更新处理函数
        if (binding.value !== binding.oldValue) {
            const delay = binding.arg ? parseInt(binding.arg) : 300;
            const handler = binding.value;
            
            if (typeof handler !== 'function') {
                return;
            }
            
            // 清除旧的事件监听
            if (el._debounceInputHandler) {
                el.removeEventListener('input', el._debounceInputHandler);
            }
            
            // 防抖定时器
            let timeout = null;
            
            // 新的防抖处理函数
            const debouncedHandler = (value) => {
                if (timeout) {
                    clearTimeout(timeout);
                }

                timeout = setTimeout(() => {
                    handler(value);
                    timeout = null;
                }, delay);
            };
            
            // input事件处理函数
            const inputHandler = (e) => {
                debouncedHandler(e.target.value);
            };
            
            // 更新存储的函数
            el._debounceInputHandler = inputHandler;
            
            // 添加新的事件监听
            el.addEventListener('input', inputHandler);
        }
    },
    
    unmounted(el) {
        // 清理事件监听
        if (el._debounceInputHandler) {
            el.removeEventListener('input', el._debounceInputHandler);
            delete el._debounceInputHandler;
        }
    }
};

export default debounceInput;

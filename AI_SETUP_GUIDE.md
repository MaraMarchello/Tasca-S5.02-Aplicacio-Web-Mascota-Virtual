# 🤖 AI Code Helper - Setup & Troubleshooting Guide

## 🚨 Issue: "The model `gpt-4` does not exist or you do not have access to it"

### **Root Cause**
The error occurs because:
1. **GPT-4 Access**: Your OpenAI API key doesn't have access to GPT-4 model
2. **Billing Limits**: GPT-4 requires higher usage limits and billing setup
3. **Model Availability**: GPT-4 access is limited to certain API keys

### **✅ Solution: Use GPT-3.5-Turbo (Recommended)**

We've updated the system to use `gpt-3.5-turbo` by default, which is:
- ✅ **Available to all OpenAI API keys**
- ✅ **Faster response times**
- ✅ **Lower costs**
- ✅ **Excellent performance for coding tasks**

## 🔧 Setup Instructions

### **Step 1: Get OpenAI API Key**

1. **Visit**: [OpenAI API Platform](https://platform.openai.com/api-keys)
2. **Sign up/Login** to your OpenAI account
3. **Create API Key**: Click "Create new secret key"
4. **Copy the key**: Save it securely (starts with `sk-...`)

### **Step 2: Configure Backend**

#### **Option A: Environment Variable (Recommended)**
```bash
# Windows (PowerShell)
$env:OPENAI_API_KEY="sk-your-actual-api-key-here"

# Windows (Command Prompt)
set OPENAI_API_KEY=sk-your-actual-api-key-here

# Linux/Mac
export OPENAI_API_KEY="sk-your-actual-api-key-here"
```

#### **Option B: Application Configuration**
Edit `backend/backend/src/main/resources/application.yml`:
```yaml
openai:
  api:
    key: "sk-your-actual-api-key-here"  # Replace with your key
    timeout: 180
    model: gpt-3.5-turbo  # Default model (recommended)
```

### **Step 3: Verify Configuration**

1. **Start Backend**: 
   ```bash
   cd backend/backend
   ./gradlew bootRun
   ```

2. **Test Configuration**:
   ```bash
   curl http://localhost:8081/api/v1/test/config
   ```
   
   **Expected Response**:
   ```
   OpenAI Configuration:
   - API Key: sk-abc...xyz
   - Model: gpt-3.5-turbo
   - Status: Ready
   ```

3. **Test Connection**:
   ```bash
   curl http://localhost:8081/api/v1/test/connection
   ```
   
   **Expected Response**:
   ```
   Connection successful! Response: [AI response]
   ```

## 🎯 Model Options

### **Available Models**

| Model | Access Level | Speed | Cost | Best For |
|-------|-------------|-------|------|----------|
| `gpt-3.5-turbo` | ✅ All users | Fast | Low | General coding, debugging |
| `gpt-4` | ⚠️ Limited access | Slower | High | Complex reasoning |
| `gpt-4-turbo-preview` | ⚠️ Limited access | Medium | High | Latest features |

### **Changing Models**

To use a different model, set the environment variable:
```bash
# Use GPT-4 (if you have access)
export OPENAI_MODEL=gpt-4

# Use GPT-3.5-Turbo (default)
export OPENAI_MODEL=gpt-3.5-turbo
```

Or update `application.yml`:
```yaml
openai:
  api:
    model: gpt-4  # Change this line
```

## 🔍 Troubleshooting

### **Common Issues & Solutions**

#### **1. "API key not set" Error**
```bash
# Check if environment variable is set
echo $OPENAI_API_KEY  # Linux/Mac
echo $env:OPENAI_API_KEY  # Windows PowerShell
```

**Solution**: Set the environment variable or update `application.yml`

#### **2. "Invalid API key" Error**
- ✅ Verify your API key is correct
- ✅ Check for extra spaces or characters
- ✅ Ensure key starts with `sk-`
- ✅ Verify your OpenAI account has billing set up

#### **3. "Model does not exist" Error**
- ✅ Use `gpt-3.5-turbo` instead of `gpt-4`
- ✅ Check your OpenAI account's model access
- ✅ Verify billing limits and usage

#### **4. "Connection timeout" Error**
- ✅ Check internet connection
- ✅ Verify firewall settings
- ✅ Try increasing timeout in `application.yml`

#### **5. "Rate limit exceeded" Error**
- ✅ Wait a few minutes before retrying
- ✅ Check your OpenAI usage limits
- ✅ Consider upgrading your OpenAI plan

### **Debug Steps**

1. **Check Configuration**:
   ```bash
   curl http://localhost:8081/api/v1/test/config
   ```

2. **Test Connection**:
   ```bash
   curl http://localhost:8081/api/v1/test/connection
   ```

3. **Check Logs**:
   - Look for OpenAI-related errors in backend console
   - Check for network connectivity issues

4. **Verify API Key**:
   - Test your API key directly with OpenAI's API
   - Ensure billing is set up on your OpenAI account

## 🚀 Quick Start Commands

### **Complete Setup (Windows PowerShell)**
```powershell
# Set API key
$env:OPENAI_API_KEY="sk-your-api-key-here"

# Start backend
cd backend/backend
./gradlew bootRun

# In another terminal, start frontend
cd frontend
npm run dev

# Test AI functionality
# Visit: http://localhost:5173 -> AI Helper page
```

### **Complete Setup (Linux/Mac)**
```bash
# Set API key
export OPENAI_API_KEY="sk-your-api-key-here"

# Start backend
cd backend/backend
./gradlew bootRun

# In another terminal, start frontend
cd frontend
npm run dev

# Test AI functionality
# Visit: http://localhost:5173 -> AI Helper page
```

## 📊 Testing the AI Features

### **1. AI Chat Interface**
- Navigate to: `http://localhost:5173` → AI Helper
- Try asking: "How do I create a Java class?"
- Expected: Detailed AI response with code examples

### **2. Stack Trace Analysis**
- Navigate to: `http://localhost:5173` → Stack Trace
- Paste a Java error message
- Expected: AI-powered error analysis and solutions

### **3. Code Explanation**
- Open AI Helper → Code Editor
- Paste some Java code
- Click "Explain" mode and ask about the code
- Expected: Detailed code explanation

## 💡 Best Practices

### **Security**
- ✅ Never commit API keys to version control
- ✅ Use environment variables for production
- ✅ Rotate API keys regularly
- ✅ Monitor API usage and costs

### **Performance**
- ✅ Use `gpt-3.5-turbo` for faster responses
- ✅ Enable caching (already configured)
- ✅ Monitor rate limits
- ✅ Implement proper error handling

### **Cost Management**
- ✅ Monitor OpenAI usage dashboard
- ✅ Set up billing alerts
- ✅ Use appropriate models for tasks
- ✅ Implement request caching

## 📞 Support

If you're still experiencing issues:

1. **Check OpenAI Status**: [status.openai.com](https://status.openai.com)
2. **Verify Account**: [platform.openai.com/account](https://platform.openai.com/account)
3. **Review Usage**: [platform.openai.com/usage](https://platform.openai.com/usage)
4. **Check Billing**: [platform.openai.com/account/billing](https://platform.openai.com/account/billing)

---

**🎉 Once configured correctly, you'll have a fully functional AI Code Helper with:**
- Real-time AI assistance for coding questions
- Intelligent error analysis and debugging help
- Code explanation and generation capabilities
- Multi-language programming support
- Context-aware responses based on your code 
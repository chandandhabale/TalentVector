# AI-Powered Resume Analyzer & RAG Chatbot

A production-ready backend application built with **Spring Boot**, **Spring AI**, **VectorStore**, and **Apache Tika**. This project provides APIs for:

* Resume text extraction
* AI-based resume analysis
* ATS score calculation
* RAG-powered PDF-based chatbot
* General AI chat interface

The system integrates AI with traditional backend engineering to create a powerful resume evaluation and document-aware chatbot.

---

## 🚀 Features

### **1. Resume Analyzer (PDF → Text → AI Evaluation)**

* Extracts resume text using **Apache Tika**
* Identifies key skills, improvements, and ratings
* Returns clean, structured JSON responses

### **2. ATS Checker (Resume vs Job Description)**

* Compares uploaded resume with provided job description
* Generates an **ATS score (0–100)**
* Lists matched & missing keywords
* Provides a short summary for improvement

### **3. RAG Chatbot (Context-Aware Document QA)**

* Uses **SimpleVectorStore** to store embeddings
* Answers questions **strictly from PDF content**
* Uses `QuestionAnswerAdvisor` + semantic search

### **4. General AI Chatbot**

* Pure LLM responses using Spring AI
* Stateless, fast, and simple

---

## 📌 Tech Stack

* **Java 21**
* **Spring Boot 3.5+**
* **Spring AI** (ChatClient, Embeddings, Vector Store, Advisors)
* **Apache Tika** (Document parsing)
* **SimpleVectorStore (JSON-based vector DB)**
* **Spring Security (optional)**
* **REST APIs**
* **Postman** (API testing)

---

## 📁 Project Structure

```
src/main/java/com/.../controller
    └── ChatController.java

src/main/java/com/.../config
    ├── ChatConfig.java
    ├── WebConfig.java

src/main/java/com/.../advisor
    └── LoggingAdvisor.java

src/main/resources
    ├── input-data/        # PDF files for RAG
    └── output-data/       # vectorstore.json
```

---

## 🔗 API Endpoints

### **1. Health Check**

```
GET /api/health
```

### **2. General Chatbot**

```
POST /api/chat
{
  "message": "Hello"
}
```

### **3. Direct Question**

```
GET /api/ask?question=What+is+Spring+AI
```

### **4. RAG Chatbot**

```
POST /api/rag/chat
{
  "message": "What is mentioned about microservices in the PDF?"
}
```

### **5. Resume Analyzer**

```
POST /api/analyze
Content-Type: multipart/form-data
file = <resume.pdf>
```

### **6. ATS Checker**

```
POST /api/ats-check
Content-Type: multipart/form-data
file = <resume.pdf>
jd = "Your Job Description..."
```

---

## 📦 Vector Store (RAG)

The application automatically:

1. Reads PDFs from `src/main/resources/input-data/`
2. Extracts text using Tika
3. Splits into chunks using `TokenTextSplitter`
4. Generates embeddings
5. Saves vector DB as `vectorstore.json`

This ensures fast, accurate document-based QA.

---

## 🧪 Testing with Postman

Each endpoint supports manual testing through Postman:

* Use **form-data** for file uploads
* Use **raw JSON** for chat endpoints
* Verify vector store by calling RAG endpoint

---

## 📘 How to Run

### **1. Clone the project**

```
git clone <repo-url>
cd project-folder
```

### **2. Configure application properties**

Add your OpenAI / API provider keys.

### **3. Run the application**

```
mvn spring-boot:run
```

### **4. Test APIs** using Postman or cURL.

---

## 🌟 Why This Project Is Valuable

This backend demonstrates:

* AI + backend integration
* RAG architecture in production
* PDF processing & embeddings
* API design & error handling
* Vector databases in Spring Boot

Perfect for showcasing backend + AI skills on your resume.

---

## ✨ Author

**Chandan Dhabele**
Java Backend Developer | Spring Boot | Microservices | AI Integrations

---


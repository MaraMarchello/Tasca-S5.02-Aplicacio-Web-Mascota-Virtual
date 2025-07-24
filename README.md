# CodeMate - AI-Powered Virtual Pet Coding Companion

![CodeMate Banner](https://img.shields.io/badge/CodeMate-Virtual%20Pet%20Companion-blue?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-0.1.0-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 🐾 Overview

CodeMate is an innovative full-stack web application that combines the joy of virtual pet care with the productivity of coding. It features an AI-powered coding assistant and a virtual pet that grows stronger as you write better code, complete coding challenges, and learn new programming concepts.

## ✨ Key Features

### 🤖 AI-Powered Coding Assistant
- **Real-time Code Help**: Get instant programming assistance powered by OpenAI GPT
- **Error Analysis**: Intelligent stack trace analysis and debugging suggestions
- **Git Coach**: Advanced Git workflow guidance and error resolution
- **Multi-language Support**: Support for various programming languages

### 🐱 Virtual Pet System
- **Interactive Pet**: Care for your virtual coding companion
- **Pet Evolution**: Your pet grows stronger with your coding progress
- **Customization**: Personalize your pet's appearance and accessories
- **Shop System**: Earn coins through coding activities to buy pet items

### 📊 Progress Tracking
- **Achievement System**: Unlock achievements for coding milestones
- **Dashboard Analytics**: Track your coding progress and pet's growth
- **Leaderboards**: Compete with other developers (Admin features)

### 🔒 User Management
- **Authentication**: Secure JWT-based authentication
- **Role-based Access**: User and Admin role management
- **Profile Management**: Customize your developer profile

## 🏗️ Architecture

### Frontend (React + TypeScript)
- **Framework**: React 18 with TypeScript
- **Styling**: Tailwind CSS with custom components
- **State Management**: Redux Toolkit + React Query
- **Build Tool**: Vite
- **Code Editor**: Monaco Editor integration
- **UI Components**: Headless UI + Heroicons

### Backend (Spring Boot + Java)
- **Framework**: Spring Boot 3.2.3 with Java 21
- **Database**: PostgreSQL with Spring Data JPA
- **Caching**: Redis for performance optimization
- **Security**: Spring Security with JWT tokens
- **AI Integration**: OpenAI GPT API integration
- **Migration**: Flyway for database versioning

### Infrastructure
- **Containerization**: Docker Compose for development
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Development**: Hot reload for both frontend and backend

## 🚀 Quick Start

### Prerequisites
- **Node.js** 18+ and npm
- **Java** 21+
- **Docker** and Docker Compose
- **OpenAI API Key** (for AI features)

### 1. Clone the Repository
```bash
git clone https://github.com/your-repo/Tasca-S5.02-Aplicacio-Web-Mascota-Virtual.git
cd Tasca-S5.02-Aplicacio-Web-Mascota-Virtual
```

### 2. Environment Setup

#### Backend Configuration
1. Create your OpenAI API key at [OpenAI Platform](https://platform.openai.com/api-keys)
2. Set environment variable:
```bash
# Linux/Mac
export OPENAI_API_KEY="sk-your-actual-api-key-here"

# Windows (PowerShell)
$env:OPENAI_API_KEY="sk-your-actual-api-key-here"
```

#### Database Setup
```bash
cd backend/backend
docker-compose up -d  # Starts PostgreSQL and Redis
```

### 3. Backend Setup
```bash
cd backend/backend
./gradlew bootRun  # Linux/Mac
gradlew.bat bootRun  # Windows
```

### 4. Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

### 5. Access the Application
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **Database**: PostgreSQL on localhost:5432

## 📚 Usage Guide

### Getting Started
1. **Sign Up**: Create your developer account
2. **Create Pet**: Design your virtual coding companion
3. **Start Coding**: Use the AI assistant for help with your projects
4. **Earn Rewards**: Complete coding challenges to earn coins
5. **Care for Pet**: Use coins to buy food, toys, and accessories

### AI Assistant Features
- **Code Help**: Ask questions about programming concepts
- **Error Analysis**: Paste stack traces for intelligent debugging
- **Git Guidance**: Get help with Git commands and workflows
- **Code Review**: Request code improvement suggestions

### Pet Care System
- **Feeding**: Keep your pet healthy with regular feeding
- **Playing**: Interactive games to keep your pet happy
- **Shopping**: Buy accessories and items from the pet shop
- **Evolution**: Watch your pet grow as you improve your coding skills

## 🛠️ Development

### Project Structure
```
├── frontend/                 # React TypeScript frontend
│   ├── src/
│   │   ├── components/       # Reusable UI components
│   │   ├── pages/           # Main application pages
│   │   ├── utils/           # Utility functions and API calls
│   │   └── contexts/        # React contexts
│   └── package.json
├── backend/backend/         # Spring Boot backend
│   ├── src/main/java/       # Java source code
│   ├── src/main/resources/  # Configuration files
│   └── build.gradle.kts
└── README.md
```

### Available Scripts

#### Frontend
```bash
npm run dev      # Start development server
npm run build    # Build for production
npm run test     # Run tests
npm run lint     # Run ESLint
npm run format   # Format code with Prettier
```

#### Backend
```bash
./gradlew bootRun        # Start Spring Boot application
./gradlew test           # Run tests
./gradlew build          # Build JAR file
./gradlew flywayMigrate  # Run database migrations
```

### API Documentation
The backend provides a RESTful API with the following main endpoints:
- `/api/auth/*` - Authentication endpoints
- `/api/users/*` - User management
- `/api/pets/*` - Pet management
- `/api/ai/*` - AI assistant endpoints
- `/api/shop/*` - Shop and inventory management

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow TypeScript best practices for frontend
- Use Java coding conventions for backend
- Write tests for new features
- Update documentation as needed
- Follow the existing code style

## 📋 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🎯 Roadmap

### Phase 1: Core Features ✅
- [x] User authentication and management
- [x] Basic pet creation and care system
- [x] AI integration with OpenAI GPT
- [x] Shop and inventory system

### Phase 2: Enhanced AI Features
- [ ] Code completion suggestions
- [ ] Advanced error prediction
- [ ] Custom AI training for specific languages
- [ ] AI-powered code reviews

### Phase 3: Social Features
- [ ] Friend system and pet interactions
- [ ] Coding challenges and competitions
- [ ] Community pet showcases
- [ ] Collaborative coding sessions

### Phase 4: Mobile & Advanced Features
- [ ] Mobile app development
- [ ] Advanced analytics and insights
- [ ] Integration with popular IDEs
- [ ] Machine learning for personalized suggestions

## 🆘 Support

For support and questions:
- Check the [AI Setup Guide](AI_SETUP_GUIDE.md) for common issues
- Review the [AI Integration Documentation](AI_INTEGRATION_IMPLEMENTATION.md)
- Open an issue on GitHub
- Contact the development team

## 🙏 Acknowledgments

- OpenAI for providing the GPT API
- The Spring Boot and React communities
- All contributors and testers

---

**Happy Coding with your CodeMate! 🐾💻**
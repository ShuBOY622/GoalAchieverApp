# Git Repository Setup Guide

This guide will help you initialize a Git repository and push your Goal App project to GitHub.

## 📋 Prerequisites

- Git installed on your system
- GitHub account created
- SSH key configured (recommended) or HTTPS access

## 🚀 Step-by-Step Git Setup

### 1. Initialize Git Repository

```bash
# Navigate to your project root directory
cd /Users/shuboy62/Documents/SpringProjects/GoalApp

# Initialize Git repository
git init

# Check Git status
git status
```

### 2. Configure Git (if not already done)

```bash
# Set your name and email (replace with your details)
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Verify configuration
git config --list
```

### 3. Add Files to Repository

```bash
# Add all files (respecting .gitignore)
git add .

# Check what files are staged
git status

# If you want to see what files are being ignored
git status --ignored
```

### 4. Create Initial Commit

```bash
# Create your first commit
git commit -m "Initial commit: Goal App microservices with Docker and AWS deployment"

# Verify commit
git log --oneline
```

### 5. Create GitHub Repository

**Option A: Using GitHub CLI (if installed)**
```bash
# Install GitHub CLI if not installed
# macOS: brew install gh
# Login to GitHub
gh auth login

# Create repository
gh repo create GoalApp --public --description "Goal tracking app with microservices architecture"

# Push to GitHub
git push -u origin main
```

**Option B: Using GitHub Web Interface**
1. Go to https://github.com
2. Click "New repository"
3. Repository name: `GoalApp`
4. Description: `Goal tracking app with microservices architecture`
5. Choose Public or Private
6. **DO NOT** initialize with README, .gitignore, or license (we already have these)
7. Click "Create repository"

### 6. Connect Local Repository to GitHub

```bash
# Add remote origin (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/GoalApp.git

# Or if using SSH (recommended)
git remote add origin git@github.com:YOUR_USERNAME/GoalApp.git

# Verify remote
git remote -v

# Push to GitHub
git branch -M main
git push -u origin main
```

### 7. Verify Upload

```bash
# Check repository status
git status

# View commit history
git log --oneline

# Check remote branches
git branch -a
```

## 📁 What Gets Uploaded

### ✅ Files That Will Be Uploaded:
- All source code files (.java, .tsx, .ts, .js)
- Configuration files (.yml, .json, .xml)
- Docker files (Dockerfile, docker-compose.yml)
- AWS CloudFormation templates
- Jenkins pipeline files
- Documentation (.md files)
- Scripts (.sh files)
- Package files (pom.xml, package.json)

### ❌ Files That Will Be Ignored:
- Compiled files (*.class, *.jar)
- Build directories (target/, build/, node_modules/)
- IDE files (.idea/, .vscode/)
- Log files (*.log)
- OS files (.DS_Store, Thumbs.db)
- Environment-specific configs (.env.local, .env.production)
- Database files (*.db, *.sqlite)
- Certificates and keys (*.pem, *.key)

## 🔧 Useful Git Commands

### Daily Development Commands

```bash
# Check status
git status

# Add specific files
git add filename.java
git add goal-app-backend/user-service/

# Add all changes
git add .

# Commit changes
git commit -m "Add user authentication feature"

# Push changes
git push

# Pull latest changes
git pull

# View commit history
git log --oneline --graph
```

### Branch Management

```bash
# Create and switch to new branch
git checkout -b feature/new-feature

# Switch between branches
git checkout main
git checkout feature/new-feature

# List all branches
git branch -a

# Merge branch to main
git checkout main
git merge feature/new-feature

# Delete branch
git branch -d feature/new-feature
```

### Viewing Changes

```bash
# See what changed
git diff

# See staged changes
git diff --cached

# See changes in specific file
git diff filename.java

# View file history
git log --follow filename.java
```

## 🔒 Security Best Practices

### 1. Sensitive Information
- Never commit passwords, API keys, or secrets
- Use environment variables for sensitive data
- Review .gitignore files regularly

### 2. SSH Keys (Recommended)
```bash
# Generate SSH key (if you don't have one)
ssh-keygen -t ed25519 -C "your.email@example.com"

# Add SSH key to ssh-agent
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519

# Copy public key to clipboard (macOS)
pbcopy < ~/.ssh/id_ed25519.pub

# Add the key to your GitHub account at:
# https://github.com/settings/ssh/new
```

### 3. Two-Factor Authentication
- Enable 2FA on your GitHub account
- Use personal access tokens for HTTPS authentication

## 🚨 Troubleshooting

### Common Issues and Solutions

**1. Permission denied (publickey)**
```bash
# Test SSH connection
ssh -T git@github.com

# If fails, check SSH key setup
ssh-add -l
```

**2. Repository already exists**
```bash
# If you need to force push (be careful!)
git push -f origin main
```

**3. Large files warning**
```bash
# Check file sizes
find . -size +50M -type f

# Use Git LFS for large files if needed
git lfs track "*.jar"
git add .gitattributes
```

**4. Merge conflicts**
```bash
# View conflicted files
git status

# Edit files to resolve conflicts
# Then add and commit
git add .
git commit -m "Resolve merge conflicts"
```

## 📊 Repository Structure After Upload

```
GoalApp/
├── .git/                     # Git metadata (hidden)
├── .gitignore               # Git ignore rules
├── .env                     # Environment variables
├── README.md                # Project overview
├── DEPLOYMENT_GUIDE.md      # Deployment instructions
├── GIT_SETUP.md            # This file
├── docker-compose.yml       # Docker Compose configuration
├── Jenkinsfile             # Main CI/CD pipeline
├── aws/                    # AWS CloudFormation templates
├── config/                 # Configuration files
├── scripts/                # Deployment scripts
├── goal-app-backend/       # Backend microservices
│   ├── api-gateway/
│   ├── user-service/
│   ├── goal-service/
│   ├── points-service/
│   ├── notification-service/
│   ├── challenge-service/
│   └── common/
└── goal-app-frontend/      # React frontend
```

## 🎯 Next Steps After Git Setup

1. **Set up branch protection rules** on GitHub
2. **Configure webhooks** for Jenkins integration
3. **Set up GitHub Actions** (optional, alternative to Jenkins)
4. **Create issues and project boards** for task management
5. **Invite collaborators** if working in a team

## 📞 Support

If you encounter issues:
1. Check GitHub's documentation: https://docs.github.com
2. Use `git help <command>` for command-specific help
3. Check Git status with `git status` when in doubt

---

**Happy coding and version controlling! 🚀**
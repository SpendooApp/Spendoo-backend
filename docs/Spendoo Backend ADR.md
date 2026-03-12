# Spendoo Backend ADR 

## Context & Overview

The Spendoo backend must support both stable financial transactions and experimental AI-driven features. To balance stability with innovation, the architecture is split into two distinct services:

1.  **Core Spendoo Service (Modular Monolith):** Handles user and financial logic. It consists of internal modules (Transactions, Budgets, Gamification, etc.) that communicate via an internal event system.
    
2.  **AI Service:** A standalone service for compute-intensive features like forecasting, OCR, and chat.

## Rationale

-   **Separation of Concerns:** Isolates stable transactional logic from rapidly evolving, experiment-heavy AI components.
    
-   **Independent Scalability:** Allows the AI service to scale independently (e.g., using GPU-optimized instances) without over-provisioning the core transaction engine.
    
-   **Technology Flexibility:** Enables the AI team to use data-science-friendly stacks (Python, PyTorch) while the Core service uses high-integrity stacks (spring boot).
    
-   **Controlled Complexity:** The Modular Monolith approach for Core Service simplifies data consistency and avoids the "microservices tax" of distributed transactions at this stage.

## The Architecture

### 1. Core Spendoo Service

_One deployable unit containing:_

-   **Transactions:** Add, edit, and categorize spending.
    
-   **Budgets & Goals:** Progress tracking.
    
-   **Gamification:** Badges and rewards.
    
-   **Pattern & Subscription Tracker:** Finds recurring bills.
    
-   **Authentication:** Security.

-   **Account Access:** User permissions (e.g. following & followers)
    
-   **Notifications & Alert:** Email and push alerts.
    
-   **Smart Savings:** Suggests deals and saving methods.
    

### 2. AI Service

_Standalone service containing:_

-   **Analytics & Forecasting:** Spending predictions.
    
-   **AI Chat Agent:** LLM-based user assistance.
    
-   **OCR:** Receipt image processing.
    
-   **Voice Input:** Natural language command understanding.

## Consequences

### ✅ Positive

-   **Clear Bounded Contexts:** Intuitive division between "financial logic" and "intelligence."
    
-   **Focused Iteration:** AI features can be deployed without risking the core payment pipelines.
    
-   **Resilience:** A failure in the AI service (e.g., OCR model timeout) does not prevent users from viewing their budgets or logging transactions manually.
    

### ⚠️ Negative / Trade-offs

-   **Operational Overhead:** Requires managing, deploying, and monitoring two separate environments.
    
-   **Network Complexity:** Introduces latency and potential failure modes via inter-service communication.

## Alternatives Considered

1.  **Pure Monolith:** Rejected. Tightly couples AI dependencies with business logic, making the deployment package bloated and limiting technology choices.
    
2.  **Full Microservices:** Rejected. Breaking every module (e.g., Notifications, Budgets) into its own service was determined to be too complex for the current team size and scale.

## Validation & Compliance

-   **Validation:** We will confirm the architecture works by observing two key flows. First, we'll verify the internal communication by ensuring the **Subscription Tracker** automatically detects recurring bills whenever the **Transaction module** announces a new entry. Second, we will verify the service-to-service connection by ensuring a **Receipt Scan** can travel from the Mobile App to the Core Service, over to the AI Service for reading, and back to the Core Service for final storage.
    
-   **Compliance:** All communication between services must be authenticated.
    
-   **Future Proofing:** The internal event bus must be documented to allow a future shift to an external broker like Kafka.

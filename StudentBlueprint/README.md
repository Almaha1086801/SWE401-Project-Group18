# Student Blueprint

AI-Powered Skill Gap and Career Roadmap System for University Students.
SWE401 Software Engineering, Fall 2026, Group 18.

## Team members

| Name | Student ID |
|---|---|
| Sara Akbar Ali | 1100830 |
| Almaha AlObeidli | 1086801 |

## Problem

Many students know which career they want but do not know which skills they are missing, or in what order to learn them. The information is spread across job sites, online courses and advisors. Student Blueprint brings it together in one place.

## Key features

- **Skill gap analysis:** compares the student's skills, courses, projects and certifications with what their chosen career needs, and shows the skills they have, need to improve and are missing. An AI explains the result in simple words.
- **Career roadmap:** turns the gaps into numbered steps in the right learning order, with a learning tip for each step.
- **Progress tracking:** the student marks steps as done, the progress bar updates and the skill level in the profile goes up.
- Register and log in with secure passwords, a 30-minute timeout and a lock after 5 wrong attempts.
- Profile with major, career goal, skills (technical and soft), courses, projects, certifications and experience.
- Read-only share link for an academic advisor, which the student turns on and off.
- 6 careers, with example internships, certifications and jobs for each. New careers can be added in the data files without changing code.

## Technologies

- Java 17 (no frameworks)
- Java's built-in web server
- HTML and CSS for the pages
- CSV files for saving data
- Google Gemini API for the AI, with saved tips if the AI is not available
- JUnit 5 for testing
- Eclipse IDE

## Prerequisites

- Eclipse IDE for Java Developers. It includes Java 17 or newer and JUnit 5.
- Optional: a free Gemini API key from Google AI Studio.

## Installation

1. Put the `StudentBlueprint` folder in your Eclipse workspace folder.
2. In Eclipse: **File → Import → General → Existing Projects into Workspace**.
3. Choose the `StudentBlueprint` folder and click **Finish**.

## Configuration

The AI is optional. Without a key, the app uses saved tips.

To turn it on:
1. **Run → Run Configurations → Main → Environment → Add**.
2. Set the name to `GEMINI_API_KEY` and the value to your key.

Do not put the key in the code.

## How to run

1. Right-click `src/com/studentblueprint/Main.java` → **Run As → Java Application**.
2. Open **http://localhost:8080** in your browser.
3. Create an account, fill in your profile, then open **My plan**.

## How to test

Right-click the `test` folder → **Run As → JUnit Test**. All 8 tests should pass.

- `BackendTest` (6 tests): skill gap groups, roadmap order, progress percentage, AI fallback, password hashing.
- `WebFlowTest` (2 tests): pages need login, and the full flow from register to marking a step done.

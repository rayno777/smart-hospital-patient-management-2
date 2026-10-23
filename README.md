# Smart Hospital Patient Management System

**Data Structures Mini Project — Raino Binoy**

A beginner-friendly web-based demonstration of how data structures can organise patient records, appointments, emergency cases and medical history.

## Technology used
- **HTML** — page structure
- **CSS** — responsive dashboard design
- **JavaScript** — form handling, table updates and API requests
- **Java (JDK 17 or later)** — built-in HTTP server and backend API
- **Java Collections** — ArrayList, HashMap, Queue/LinkedList and PriorityQueue

## Features
1. Register patient records and search by ID/name.
2. Book appointments in a first-in, first-out queue.
3. Register emergency cases with severity from 1 to 5; severity 5 is shown first.
4. View a patient's activity history.
5. Dashboard statistics for patients, appointments and emergencies.

## Folder structure
```text
Smart_Hospital_DS_Mini_Project/
├── README.md
├── run.bat
├── run.sh
├── src/
│   └── HospitalServer.java
└── public/
    ├── index.html
    ├── style.css
    └── app.js
```

## Requirements
Install **Java JDK 17 or newer**. Check your installation by running:
```bash
java -version
javac -version
```

## How to run (Windows)
1. Extract the project ZIP.
2. Open the extracted `Smart_Hospital_DS_Mini_Project` folder.
3. Double-click `run.bat`, or open Command Prompt in that folder and run:
   ```bat
   run.bat
   ```
4. Open your browser and visit **http://localhost:8080**.
5. Keep the terminal open while using the website. Press `Ctrl+C` to stop.

## How to run (macOS / Linux)
1. Open a terminal inside the extracted project folder.
2. Run:
   ```bash
   chmod +x run.sh
   ./run.sh
   ```
3. Open **http://localhost:8080** in your browser.

You can also run manually:
```bash
javac -d out src/HospitalServer.java
java -cp out HospitalServer
```

## How to demonstrate the project
1. Open **Patients** and register a demo patient. Note the generated ID (for example, `P1001`).
2. Open **Appointments**, enter that patient ID, and book an appointment.
3. Open **Emergency Queue**, use the same ID and select a severity level.
4. Add another emergency with a different severity and observe that the highest severity is listed first.
5. Open **Medical History**, enter the patient ID to see recorded activity.
6. Use the dashboard to show the updated counts.

## Data structures and their purpose
| Data structure | Where it is used | Purpose |
|---|---|---|
| ArrayList | Patient records | Stores patient records in a list |
| HashMap | Patient ID lookup | Finds a patient record by ID |
| Queue (LinkedList) | Appointments | Preserves first-in, first-out order |
| PriorityQueue | Emergency cases | Orders cases by severity |
| LinkedList | Medical history | Stores recent activity entries |
| Searching | Patient screen | Filters patient IDs and names |
| Sorting | Emergency queue display | Displays high severity first |

## Important notes / limitations
- This is an **educational prototype**, not a real hospital system.
- Data is held in memory, so records reset when the Java server is restarted.
- Do not enter real patient details or sensitive medical information.
- The server is intended for local demonstration, not public deployment.
- No authentication, database, encryption or real clinical workflow is implemented.

## GitHub upload checklist
1. Create a new GitHub repository named `smart-hospital-patient-management`.
2. Set visibility to **Public**.
3. Upload all files and folders from this project, or use Git:
   ```bash
   git init
   git add .
   git commit -m "Add Smart Hospital DS mini project"
   git branch -M main
   git remote add origin https://github.com/YOUR-USERNAME/smart-hospital-patient-management.git
   git push -u origin main
   ```
4. Replace `YOUR-USERNAME` with your actual GitHub username.
5. Open the repository page and confirm the files are visible.
6. Copy the repository URL into your class Google Sheet.


## GitHub Pages version (fixed)

GitHub Pages only serves static files; it does not run the Java `HospitalServer` backend.
The browser version therefore stores this demo's records in `localStorage` on the device/browser
that opens the site. Patient registration, searching, appointments, emergency priority ordering,
and medical-history entries work without a backend.

**Important:** this is a student demo only. Data is stored only in that browser on that device,
is not shared with other users/devices, and may be removed if browser site data is cleared.
Do not enter real patient or sensitive medical information.

After replacing `public/app.js` in your GitHub repository, commit and push the change. Refresh
the GitHub Pages website; if the old version remains, clear the browser cache or try a private tab.

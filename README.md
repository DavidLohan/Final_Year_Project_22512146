# README

This is my README file

---

## Running the Canvas

As of rn to run the canvas run:

```
mvn clean javafx:run
```

---

## Running the Machine Learning API

In the ML folder terminal may not be in the right path so gotta
cd into the ML folder.

### Step 1: Activate Virtual Environment

```
.\.venv\Scripts\Activate.ps1
```

### Step 2: Run the API

```
uvicorn api:app --reload --port 8000
```

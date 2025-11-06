# Testing and Code Review

## 1. Change History

| **Change Date**   | **Modified Sections** | **Rationale** |
| ----------------- | --------------------- | ------------- |
| _Nothing to show_ |

---

## 2. Back-end Test Specification: APIs

### 2.1. Locations of Back-end Tests and Instructions to Run Them

#### 2.1.1. Tests

| **Interface**                 | **Describe Group Location, No Mocks**                | **Describe Group Location, With Mocks**            | **Mocked Components**              |
| ----------------------------- | ---------------------------------------------------- | -------------------------------------------------- | ---------------------------------- |
| **POST /auth/signup**          | [`backend/tests/unmocked/authNM.test.ts#L48`] | [`backend/tests/mocked/authM.test.ts#L21`] | Google Authentication API/ Auth Service |
| **POST /auth/signin**          | [`backend/tests/unmocked/authNM.test.ts#L463`] | [`backend/tests/mocked/authM.test.ts#L107`] | Google Authentication API/ Auth Service |
| **GET /buddy**                 | [`backend/tests/unmocked/buddyNM.test.ts#L64`] | [`backend/tests/mocked/buddyM.test.ts#L66`] | User DB |
| **GET /chats**                 | [`backend/tests/unmocked/chatNM.test.ts#L231`] | [`backend/tests/mocked/chatM.test.ts#L91`] | Chat DB |
| **POST /chats**                 | [`backend/tests/unmocked/chatNM.test.ts#L139`] | [`backend/tests/mocked/chatM.test.ts#L218`] | Chat DB |
| **GET /chats/:chatId**             | [`backend/tests/unmocked/chatNM.test.ts#L264`] | [`backend/tests/mocked/chatM.test.ts#L145`] | Chat DB |
| **GET /chats/messages/:chatId**      | [`backend/tests/unmocked/chatNM.test.ts#L426`] | [`backend/tests/mocked/chatM.test.ts#L440`] | Chat DB |
| **POST /chats/:chatId/messages/**     | [`backend/tests/unmocked/chatNM.test.ts#L329`] | [`backend/tests/mocked/chatM.test.ts#L317`] | Chat DB |
| **GET /events**     | [`backend/tests/unmocked/eventNM.test.ts#L66`] | [`backend/tests/mocked/eventM.test.ts#L68`] | Event DB |
| **GET /events/:eventId**     | [`backend/tests/unmocked/eventNM.test.ts#L79`] | [`backend/tests/mocked/eventM.test.ts#L128`] | Event DB |
| **POST /events**     | [`backend/tests/unmocked/eventNM.test.ts#L134`] | [`backend/tests/mocked/eventM.test.ts#L208`] | Event DB |
| **PUT /events/join/:eventId**     | [`backend/tests/unmocked/eventNM.test.ts#L232`] | [`backend/tests/mocked/eventM.test.ts#L433`] | Event DB |
| **PUT /events/leave/:eventId**     | [`backend/tests/unmocked/eventNM.test.ts#L279`] | [`backend/tests/mocked/eventM.test.ts#L551`] | Event DB |
| **PUT /events/:eventId**     | [`backend/tests/unmocked/eventNM.test.ts#L326`] | [`backend/tests/mocked/eventM.test.ts#L303`] | Event DB |
| **DELETE /events/:eventId**     | [`backend/tests/unmocked/eventNM.test.ts#L412`] | [`backend/tests/mocked/eventM.test.ts#L665`] | Event DB |
| **POST /media/upload**     | [`backend/tests/unmocked/mediaNM.test.ts#L130`] | [`backend/tests/mocked/mediaM.test.ts#L136`] | Media Service |
| **GET /users**     | [`backend/tests/unmocked/userNM.test.ts#L80`] | [`backend/tests/mocked/userM.test.ts#L70`] | User DB |
| **GET /users/profile**     | [`backend/tests/unmocked/userNM.test.ts#L93`] | [`backend/tests/mocked/userM.test.ts#L96`] | User DB |
| **GET /users/:id**     | [`backend/tests/unmocked/userNM.test.ts#L108`] | [`backend/tests/mocked/userM.test.ts#L110`] | User DB |
| **DELETE /users/**     | [`backend/tests/unmocked/userNM.test.ts#L432`] | [`backend/tests/mocked/userM.test.ts#L360`] | User DB |
| **DELETE /users/:id**     | [`backend/tests/unmocked/userNM.test.ts#L369`] | [`backend/tests/mocked/userM.test.ts#L298`] | User DB |
| **PUT /users/:id**     | [`backend/tests/unmocked/userNM.test.ts#L154`] | [`backend/tests/mocked/userM.test.ts#L151`] | User DB |
| **POST /users/**     | [`backend/tests/unmocked/userNM.test.ts#L257`] | [`backend/tests/mocked/userM.test.ts#L235`] | User DB |

#### 2.1.2. Commit Hash Where Tests Run

`[Insert Commit SHA here]`

#### 2.1.3. Explanation on How to Run the Tests

1. **Clone the Repository**:

   - Open your terminal and run:
     ```
     git clone https://github.com/DiveBuddy-321/DiveBuddy.git
     ```

2. **Enter the Backend Directory**:

   - Navigate to the backend directory:
     ```
     cd DiveBuddy/backend
     ```

3. **Create a .env File**:

   - Create a `.env` file in the root of the `backend` directory with the following content:
     ```
     JWT_SECRET=your_test_secret_key
     MONGODB_URI=mongodb://localhost:27017/
     ```
   - Replace `your_test_secret_key` with a valid secret key for testing purposes (can use the same one as in your own project).

4. **Install Dependencies**:
    - Install the required packages using npm:
      ```
      npm install
      ```

5. **Run the Tests**:
    - To run the tests without mocking, execute:
      ```
      npx jest /unmocked --runInBand --coverage
      ```
    - To run the tests with mocking, execute:
      ```
      npx jest /mocked --runInBand --coverage
      ``` 
    - To execute non-functional requirement tests, run:
      ```
      npx jest /nfr --runInBand --coverage
      ```
    - To run all tests, execute:
      ```
      npx jest --runInBand --coverage
      ```
    - To run a specific test file, (for example nfr1.test.ts), execute:
      ```
      npx jest nfr1 --runInBand --coverage
      ```



### 2.2. GitHub Actions Configuration Location

`~/.github/workflows/test.yml`

### 2.3. Jest Coverage Report Screenshots for Tests Without Mocking

![Unmocked Coverage](../documentation/images/unmocked_coverage.png)

### 2.4. Jest Coverage Report Screenshots for Tests With Mocking

![Mocked Coverage](../documentation/images/mocked_coverage.png)

### 2.5. Jest Coverage Report Screenshots for Both Tests With and Without Mocking

![Total Coverage](../documentation/images/total_coverage.png)

Total coverage is only around 85% because some files such as index.ts, routes.ts, storage.ts and database.ts are involved with setting up the server and routing, and are not directly tested by any test suites.
Furthermore, the authentication middleware in authMiddleware.ts is not directly tested, because many of the functions need a valid Google OAuth token to work, which is difficult to simulate in tests. The authentication middleware is mainly mocked throughout the tests, and as a result, the coverage for that file is low. In the models and controller files, some lines are not directly reached in the tests because 
they are error handling code for edge cases that are often caught in other parts of the code. All test cases testing each endpoint focus on all success and failures scenarios, but some specific error handling code is not directly reached because of being caught in other areas of the code. The other files that have low coverage are utility files that contain helper functions that are not directly used when calling the 
exposed backend APIs, so their coverage is low as well.

## 3. Back-end Test Specification: Tests of Non-Functional Requirements

### 3.1. Test Locations in Git

| **Non-Functional Requirement**  | **Location in Git**                              |
| ------------------------------- | ------------------------------------------------ |
| **Buddy Matching Response Times** | [`backend/tests/nfr/nfr1.test.js`]|
| **Auth/Chat/User/ChatAPI Response Times**          | [`backend/tests/nfr/nfr2.test.js`] |

### 3.2. Test Verification and Logs

- **Buddy Matching Response Times**

  - **Verification:** This test suite populates the test database with 10000 simulated users and then calls the buddy matching API endpoint to ensure that the response time is within 1s, as our non-functional requirement specifies.
  - **Log Output**

    ![NFR1 log](../documentation/images/nfr1_log.png)

- **Auth/Chat/User/ChatAPI Response Times**
  - **Verification:** This test suite tests every API endpoint related to authentication, chat, user management, and event management to ensure that each endpoint responds within 500ms, as specified in our non-functional requirements.
  - **Log Output**

    ![NFR2 auth log](../documentation/images/nfr2_auth.png)

    ![NFR2 chats log](../documentation/images/nfr2_chats.png)

    ![NFR2 events log](../documentation/images/nfr2_events.png)

    ![NFR2 users log](../documentation/images/nfr2_users.png)
---

## 4. Front-end Test Specification

### 4.1. Location in Git of Front-end Test Suite:

`frontend/src/androidTest/java/com/studygroupfinder/`

### 4.2. Tests

- **Use Case: Login**

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens "Add Todo Items" screen. | Open "Add Todo Items" screen. |
    | 2. The app shows an input text field and an "Add" button. The add button is disabled. | Check that the text field is present on screen.<br>Check that the button labelled "Add" is present on screen.<br>Check that the "Add" button is disabled. |
    | 3a. The user inputs an ill-formatted string. | Input "_^_^^OQ#$" in the text field. |
    | 3a1. The app displays an error message prompting the user for the expected format. | Check that a dialog is opened with the text: "Please use only alphanumeric characters ". |
    | 3. The user inputs a new item for the list and the add button becomes enabled. | Input "buy milk" in the text field.<br>Check that the button labelled "add" is enabled. |
    | 4. The user presses the "Add" button. | Click the button labelled "add ". |
    | 5. The screen refreshes and the new item is at the bottom of the todo list. | Check that a text box with the text "buy milk" is present on screen.<br>Input "buy chocolate" in the text field.<br>Click the button labelled "add".<br>Check that two text boxes are present on the screen with "buy milk" on top and "buy chocolate" at the bottom. |
    | 5a. The list exceeds the maximum todo-list size. | Repeat steps 3 to 5 ten times.<br>Check that a dialog is opened with the text: "You have too many items, try completing one first". |

  - **Test Logs:**
    ```
    [Placeholder for Espresso test execution logs]
    ```

- **Use Case: ...**

  - **Expected Behaviors:**

    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | ...                | ...                 |

  - **Test Logs:**
    ```
    [Placeholder for Espresso test execution logs]
    ```

- **...**

---

## 5. Automated Code Review Results

### 5.1. Commit Hash Where Codacy Ran

`a97a33a65cbd4bdae90eb20c647a90aebf6fb6d1`

### 5.2. Unfixed Issues per Codacy Category
No issues remaining as per screenshot below:
![codacy results](../documentation/images/codacy.png)

### 5.3. Unfixed Issues per Codacy Code Pattern

- Not applicable, no issues remaining as per screenshot in section above

### 5.4. Justifications for Unfixed Issues

- Not Applicable

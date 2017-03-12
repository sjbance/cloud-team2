REQUIREMENTS:

- Node.js (https://nodejs.org/)
- npm (https://www.npmjs.com/)

SETUP:

To use a specific AWS account, change the AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY .env file variables. To use a specific port, change the PORT variable.

TO RUN:

- Navigate to root folder
- Run 'npm install'
- Run 'node app.js' 

App will run on port 3001 if none is specified.

ENDPOINTS:

- POST /insurance - send information to INS web service (houseId, mortId, token)

Dev use:
- POST /service   - create service code listing (houseId, serviceCode)
- GET /service    - get a service code (houseId)
- DELETE /service - delete entry (houseId)
- GET /list       - list all entries in database



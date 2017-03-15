REQUIREMENTS:

- Node.js (https://nodejs.org/)
- npm (https://www.npmjs.com/)

SETUP:

To use a specific AWS account, change the AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY .env file variables. To use a specific port, change the PORT variable.

TO RUN:

- Navigate to root folder
- Run 'npm install'
- Run 'node app.js' 

App will run on port 3000 if none is specified. To navigate to application, the URL must include "https://" as a prefix.

The web portals can be found at the following URLs:

Jobs Inc. - https://[host_name]:[port]/emp

*Security Note*
 
As the certificate is self-signed, some browsers may block navigation/scripts. Depending on your browser, you can proceed to the site anyways and/or add the certificate to your trusted list. The key/certificate can be found in the 'ssl' folder.

ENDPOINTS:

- POST /authenticate  - authenticate the user (id, password)
- POST /mortgage  - send info to MBR service (mortId, token)

Dev use:
- POST /employee  - create employee (id, name, startOfEmployment, salary)
- GET /employee   - get an employee's record (id)
- GET /list       - list all employees in database
- GET /authlist   - list all employee logins
- DELETE /employee- delete an employee (id)


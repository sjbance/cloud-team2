Requirements:

    Node.js
    NPM
    Node packages in package.json


Setup:

    .env:

    Modify the AWS keys to represent your AWS account
    Set PORT to a port number (default is 3001).

    Advanced:

    Set the PATH environmental variables to the MBR, Log and Auth web services.


Running:

    run: "nodejs app.js"


SSL information:

    Passphrase is 4145.

    If certificates have expired, run "openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365" in the ssl directory.

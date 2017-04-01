** How to generate htpasswd:
```
    # Install htpasswd
    sudo apt-get install apache2-utils
    # Generate password
    htpasswd -Bbn <your user> <your password>
```
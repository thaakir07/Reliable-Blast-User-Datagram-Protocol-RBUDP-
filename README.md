# Reliable Blast User Datagram Protocol (RBUDP)

Java application to transfer files of any type using either TCP or RBUDP.

## Authors

- **Gideon Daniel Botha**
- **Priyal Bhana**
- **Sulaiman Bandarkar**
- **Thaakir Fernandez** - thaakir07@gmail.com

## Project Timeline

- **Start Date:** March 2025
- **Completed:** March 2025

## Directory Structure

```
.
├── src/
│   ├── Main.java        # starting point with JavaFX application with GUI
│   ├── Receiver.java    # file receiving program 
│   ├── Sender.java      # file sending program
├── Makefile             # Build and execution automation
└── README.md
```

## Prerequisites

- Java Development Kit (JDK) 8 or higher with JavaFX support
- Make utility (for using the Makefile)
- Network connectivity (eg. via ZeroTier VPN)
- JavaFX library

3. make clean: Removes *.class files.

## Compilation and Execution

This project includes a Makefile for easy compilation and execution. All source files are located in the `src/` directory.

To compile all Java files:

```
make Main
```

To run Main file

```
make run
```

### Cleaning Build Files

To remove the `bin/` directory containing compiled class files:

```
make clean
```

## Contributing

This is an academic project. For questions or issues, please contact any of the authors listed above.

## License

This project is created for educational purposes.


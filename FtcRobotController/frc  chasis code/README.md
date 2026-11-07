## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).

## Swerve drive starter code

The four-module teleop example is under `src/main/java/frc/robot`. It is for a
**WPILib FRC robot**, not the FTC project in the parent folder.

Use an Xbox-compatible controller on port 0:

- Left stick: forward/back and strafe
- Right stick X: rotate
- Left bumper: slow mode

The controls are robot-relative. The steering output uses a basic proportional
controller and must be calibrated on the actual robot before driving.

### Hardware setup still required

Before movement can be enabled, edit `Constants.java` with each module's CAN IDs
and DIO encoder channel, each encoder's zero offset, wheelbase, trackwidth, and
measured maximum speed. Check motor inversion settings against the real modules.
The default placeholder values deliberately keep the swerve disabled.

This folder does not yet include a WPILib GradleRIO project or vendor libraries.
Import these sources into a WPILib project that includes WPILib and the REVLib
Spark MAX library before building or deploying. Do not test powered motion until
motor directions, encoder readings, and steering response have been checked with
the wheels safely off the floor.

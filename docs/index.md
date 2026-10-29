---
  layout: default.md
  title: ""
---

# NUSocietyDesk

[![CI Status](https://github.com/AY2627S1-CS2103T-T17-3/tp/workflows/Java%20CI/badge.svg)](https://github.com/AY2627S1-CS2103T-T17-3/tp/actions)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-T17-3/tp/graph/badge.svg?token=X6TRWEESQX)](https://codecov.io/gh/AY2627S1-CS2103T-T17-3/tp)

**Your society's members, in one place.**

NUSocietyDesk is an offline desktop member directory for NUS society secretaries.
Keep contact details, roles, committees, and membership status together, and use typed commands
to find and update members while viewing their details in a graphical list.

[**Download NUSocietyDesk**](https://github.com/AY2627S1-CS2103T-T17-3/tp/releases)
&nbsp; | &nbsp; [**Get started**](UserGuide.html#quick-start)

![NUSocietyDesk showing sample members, contact details, society tags, ACTIVE badges, and the ALL status button](images/Ui.png)

## Spend less time on member administration

* **Bring your existing directory.** Import members from a CSV file and review feedback for any rejected records.
* **Find the right member.** Search by name, phone, email, or tag, and combine conditions to narrow your results.
* **Keep details up to date.** Add a member with just their `@u.nus.edu` email and fill in other contact details later.
* **Organize roles and committees.** Add or remove tags such as `exco`, `publicity`, and `volunteer`.
* **Retain former members.** Archive records without deleting their details, and activate returning members.
  Switch between All, Active, and Archived views using the status button or a command.

## Work from your keyboard, even offline

Type `find t/publicity` to find members with the publicity tag, or `view archived` to view archived members.
Searches apply to the selected status view. Use the ↑ and ↓ keys to recall recent commands.

Your member directory stays on your computer, and successful changes are saved automatically.
Core member-management features work without an Internet connection.

## Start managing your directory

You need Java **25 or later** to run NUSocietyDesk. Follow the
[Quick Start](UserGuide.html#quick-start) to download and launch the app with sample members,
then explore the [User Guide](UserGuide.html) for commands and CSV import instructions.

For contributors, the [Developer Guide](DeveloperGuide.html) explains the application's design and setup.

## Acknowledgements

* Libraries used: [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson), [JUnit5](https://github.com/junit-team/junit5)

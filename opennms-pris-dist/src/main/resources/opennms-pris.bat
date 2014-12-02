@echo off
cd "%~dp0"
java %* -cp "%~dp0lib*";"%~dp0opennms-pris.jar" org.opennms.pris.Starter


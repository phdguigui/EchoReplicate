#!/bin/bash

echo "🛠️ RMI Registry Iniciado"

rmiregistry -J-Djava.class.path="../run:../lib/*"
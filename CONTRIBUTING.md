# Contributing to FleetPulse

## Getting started

    git clone https://github.com/<you>/fleetpulse && cd fleetpulse
    docker compose up -d        # infrastructure
    mvn -DskipTests package     # build all modules

## Branching & commits

- `main` is protected; work on feature branches (`feat/...`, `fix/...`)
- Conventional commit messages (`feat:`, `fix:`, `style:`, `chore:`)
- CI must pass (`mvn -B verify`) before merge

## Modules

See the README for the architecture overview. Bug reports and pull
requests via GitHub Issues/PRs welcome.

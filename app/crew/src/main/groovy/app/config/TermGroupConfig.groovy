package app.config

import groovy.transform.CompileStatic

@CompileStatic
enum TermGroupConfig {
    certificate,
    product,
    plm,
    qualitySystemProcedure,
    tagsPlanet(false),
    ticketCategory(false),
    categoriesPlanet(false),
    teamRole(false),
    production,
    interestedIn(false),
    fmarketSegmentiledirectory(false)

    TermGroupConfig(boolean active = true) {
        this.active = active
    }

    final boolean active
}
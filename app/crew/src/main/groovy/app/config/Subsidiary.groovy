package app.config


import groovy.transform.CompileStatic
import org.springframework.context.i18n.LocaleContextHolder
import taack.ui.EnumOption
import taack.ui.base.common.IStyled
import taack.ui.base.common.Style
import taack.ui.config.Country

@CompileStatic
final enum SupportedCurrency {
    EUR(2),
    USD(2),
    INR(0),
    CNY(2),
    RUB(2)

    SupportedCurrency(final int precision) {
        this.precision = precision
    }

    final int precision

    static EnumOption[] getEnumOptions() {
        EnumOption[] res = new EnumOption[values().length]
        int i = 0
        for (def c in values()) {
            res[i++] = new EnumOption(c.toString(), c.toString())
        }
        return res
    }
}

@CompileStatic
enum SupportedLanguage {
    FR('fr', 'Français'),
    EN('en', 'English'),
    ES('es', 'Lengua española'),
    DE('de', 'Deutsche Sprache'),
    RU('ru', 'Русский язык'),
    IN('hi', 'Hindi'),
    PT('pt', 'Português'),
    PL('pl', 'Polski'),
    IT('it', 'Italiano'),
    CN('zh', '中文')

    SupportedLanguage(final String iso2, final String label) {
        this.iso2 = iso2
        this.label = label
    }
    final String iso2
    final String label

    static SupportedLanguage fromIso2(final String iso2) {
        values().find { it.iso2 == iso2 } ?: EN
    }

    static EnumOption[] getEnumOptions() {
        EnumOption[] res = new EnumOption[values().length]
        int i = 0
        for (def c in values()) {
            res[i++] = new EnumOption(c.iso2, c.label)
        }
        return res
    }

    static SupportedLanguage fromContext() {
        SupportedLanguage language = EN
        try {
            language = LocaleContextHolder.locale.language.split("_")[0]?.toUpperCase()?.replace("ZH", "CN") as SupportedLanguage
        } catch (ignored) {
        }
    }
}

@CompileStatic
enum Address {
    YOUR_ADDRESS(Country.US, "City", "ZIPCODE", "Street")

    Address(final Country country, final String city, final String zipCode, final String street, final String countryLocalName = null) {
        this.country = country
        this.countryLocalName = countryLocalName
        this.city = city
        this.zipCode = zipCode
        this.street = street
    }

    final String street
    final String city
    final String zipCode
    final Country country
    final String countryLocalName
}

@CompileStatic
final enum AdministrativeTax {
    YOUR_TAX('TAX LABEL', 'TAX CODE')
    AdministrativeTax(final String taxLabel, final String taxCode) {
        this.taxCode = taxCode
        this.taxLabel = taxLabel
    }

    final String taxCode
    final String taxLabel
}

@CompileStatic
final enum AdministrativeIdentifier {
    YOUR_COMPANY_IDENTIFIER("SIRET", "123123123123")

    AdministrativeIdentifier(final String idLabel, final String idCode) {
        this.idCode = idCode
        this.idLabel = idLabel
    }

    final String idCode
    final String idLabel
}
@CompileStatic
final enum Subsidiary implements IStyled {
    YOUR_COMPANY(null, 'Your Company', Address.YOUR_ADDRESS, SupportedCurrency.USD, SupportedLanguage.EN)

    Subsidiary(final Subsidiary parent, final String company, final Address address, final SupportedCurrency currency,
               final SupportedLanguage defaultLanguage, final AdministrativeIdentifier administrativeIdentifier = null,
               final AdministrativeTax administrativeTax = null, final String phone = '', final String tollFree = '',
               final String orderMail = '', final String website = '', final String logo = 'your_logo.svg',
               final Style style = null) {
        this.currency = currency
        this.parent = parent
        this.address = address
        this.style = style
        this.company = company
        this.logo = logo
        this.phone = phone
        this.tollFree = tollFree
        this.orderMail = orderMail
        this.website = website
        this.defaultLanguage = defaultLanguage
        this.administrativeIdentifier = administrativeIdentifier
        this.administrativeTax = administrativeTax
    }

    final String company
    final String logo
    final AdministrativeIdentifier administrativeIdentifier
    final AdministrativeTax administrativeTax
    final String phone
    final String tollFree
    final String orderMail
    final String website
    final SupportedCurrency currency
    final Address address
    final SupportedLanguage defaultLanguage
    final Subsidiary parent
    final Style style

    Collection<Subsidiary> getChildren() {
        values().findAll {
            it.parent == this || this.parent?.parent == this
        }
    }

    @Override
    Style getElementStyle() {
        return style
    }
}
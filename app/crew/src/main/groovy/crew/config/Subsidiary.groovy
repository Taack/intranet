package crew.config


import groovy.transform.CompileStatic
import org.springframework.context.i18n.LocaleContextHolder
import taack.config.Country
import taack.ui.EnumOptions
import taack.ui.IEnumOption
import taack.ui.IEnumOptions
import taack.ui.dsl.common.IStyled
import taack.ui.dsl.common.Style

@CompileStatic
enum SupportedCurrency {
    EUR(2),
    USD(2),
    INR(0),
    CNY(2),
    RUB(2)

    SupportedCurrency(final int precision) {
        this.precision = precision
    }

    final int precision

    static IEnumOptions getEnumOption(SupportedCurrency currency) {
        new EnumOptions(values(), 'currency', currency)
    }
}

@CompileStatic
enum SupportedLanguage implements IEnumOptions, IEnumOption {
    FR('fr', 'Français'),
    EN('en', 'English'),
    ES('es', 'Lengua española'),
    DE('de', 'Deutsche Sprache'),
    RU('ru', 'Русский язык'),
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

    static SupportedLanguage fromContext() {
        try {
            SupportedLanguage language = LocaleContextHolder.locale.language.split("_")[0]?.toUpperCase()?.replace("ZH", "CN") as SupportedLanguage
            language ?: EN
        } catch (ignored) {
            return EN
        }
    }

    @Override
    IEnumOption[] getOptions() {
        values() as IEnumOption[]
    }

    @Override
    IEnumOption[] getCurrents() {
        [fromContext()] as IEnumOption[]
    }

    @Override
    String getParamKey() {
        return 'lang'
    }

    @Override
    String getKey() {
        return iso2
    }

    @Override
    String getValue() {
        return label
    }

    @Override
    String getAsset() {
        return "taack/icons/countries/4x3/${iso2}.webp"
    }

    @Override
    Boolean isSection() {
        return null
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
enum AdministrativeTax {
    YOUR_TAX('TAX LABEL', 'TAX CODE')

    AdministrativeTax(final String taxLabel, final String taxCode) {
        this.taxCode = taxCode
        this.taxLabel = taxLabel
    }

    final String taxCode
    final String taxLabel
}

@CompileStatic
enum AdministrativeIdentifier {
    YOUR_COMPANY_IDENTIFIER("SIRET", "123123123123")

    AdministrativeIdentifier(final String idLabel, final String idCode) {
        this.idCode = idCode
        this.idLabel = idLabel
    }

    final String idCode
    final String idLabel
}

@CompileStatic
enum Subsidiary implements IStyled {
    YOUR_SUBSIDIARY1(null, 'Your Subsidiary US', Address.YOUR_ADDRESS, SupportedCurrency.USD, SupportedLanguage.EN),
    YOUR_SUBSIDIARY2(null, 'Your Subsidiary FR', Address.YOUR_ADDRESS, SupportedCurrency.EUR, SupportedLanguage.FR)

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
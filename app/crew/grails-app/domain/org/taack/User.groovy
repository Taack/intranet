package org.taack

import app.config.AttachmentType
import app.config.BusinessUnit
import app.config.Subsidiary
import app.config.SupportedLanguage
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum

@GrailsCompileStatic
@TaackFieldEnum
@EqualsAndHashCode(includes='username')
@ToString(includes='username', includeNames=true, includePackage=false)
class User implements Serializable {

    private static final long serialVersionUID = 1
    User userCreated
    Date dateCreated

    String firstName
    String lastName
    String username
    String password
    BusinessUnit businessUnit
    Subsidiary subsidiary
    SupportedLanguage language
    String mail

    User manager

    Set<Attachment> attachments

    Boolean enabled = true
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired

    Set<Role> getAuthorities() {
        (UserRole.findAllByUser(this) as List<UserRole>)*.role as Set<Role>
    }

    static hasMany = [
            attachments        : Attachment,
    ]

    static mappedBy = [attachments: 'none']

    static constraints = {
        userCreated nullable: true
        dateCreated nullable: true
        firstName nullable: true
        lastName nullable: true
        language nullable: true
        subsidiary nullable: true
        manager nullable: true
        mail nullable: true, email: true
        password nullable: false, blank: false, password: true
        username nullable: false, blank: false, unique: true
    }

    static mapping = {
        table "taack_user"
	    password column: '`password`'
    }

    Long getMainPictureId() {
        this.attachments.find { it.type == AttachmentType.mainPicture && it.active }?.id
    }

    List<User> getManagedUsers() {
        User.findAllByManagerAndEnabled(this, true)
    }

    List<User> getAllManagedUsers() {
        getAllManagedUsersFromList(managedUsers)
    }

    private static List<User> getAllManagedUsersFromList(List<User> users) {
        List<User> ret = []
        users.each {
            ret.add it
            ret.addAll getAllManagedUsersFromList(it.getManagedUsers())
        }
        ret
    }
}

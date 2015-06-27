package skinny.controller.feature

import skinny.validator._
import skinny.validator.NewValidation
import skinny.validator.MapValidator
import skinny.I18n
import java.util.Locale
import skinny.controller.{ KeyAndErrorMessages, Params }
import skinny.util.StringUtil.toCamelCase

/**
 * Validation support for Skinny app.
 */
trait ValidationFeature {

  self: org.scalatra.NonMacroDSL.ScalatraBase with RequestScopeFeature with LocaleFeature =>

  /**
   * Creates new validation form.
   *
   * @param validations validations
   * @param locale current locale
   * @return validator
   */
  def validationWithParams(validations: NewValidation*)(implicit locale: Locale = currentLocale.orNull[Locale]): MapValidator = {
    validationWithPrefix(Params(params), null, validations: _*)
  }

  /**
   * Creates new validation form.
   *
   * @param params params
   * @param validations validations
   * @param locale current locale
   * @return validator
   */
  def validation(params: Params, validations: NewValidation*)(implicit locale: Locale = currentLocale.orNull[Locale]): MapValidator = {
    validationWithPrefix(params, null, validations: _*)
  }

  /**
   * Creates new validation form.
   *
   * @param prefix key prefix for error message
   * @param params params
   * @param validations validations
   * @param locale current locale
   * @return validator
   */
  def validationWithParamsAndPrefix(prefix: String, params: Params, validations: NewValidation*)(
    implicit locale: Locale = currentLocale.orNull[Locale]): MapValidator = {
    validationWithPrefix(params, prefix, validations: _*)
  }

  /**
   * Creates new validation form.
   *
   * @param params params
   * @param prefix key prefix for error message
   * @param validations validations
   * @param locale current locale
   * @return validator
   */
  def validationWithPrefix(params: Params, prefix: String, validations: NewValidation*)(
    implicit locale: Locale = currentLocale.orNull[Locale]): MapValidator = {

    if (params == null) {
      throw new IllegalStateException("You cannot call #validation when Scalatra's #params is absent.")
    }

    val validator = new MapValidator(params.underlying, Validations(params.underlying, validations)) {
      override def validate(): Boolean = {
        if (hasErrors) {
          status = 400
          setParams()
        }
        super.validate()
      }
    }

    validator
      .success { _ => }
      .failure { (inputs, errors: Errors) =>
        val skinnyValidationMessages = Messages.loadFromConfig(locale = Option(locale))
        val i18n = I18n(locale)
        def withPrefix(key: String): String = if (prefix != null) s"${prefix}.${key}" else key

        // errorMessages
        val errorMessages: Seq[String] = {
          ValidationFeature.buildErrorMessagesWithPrefix(prefix, validations, errors)
        }
        set(RequestScopeFeature.ATTR_ERROR_MESSAGES, errorMessages)

        // keyAndErrorMessages
        val keyAndErrorMessages: KeyAndErrorMessages = {
          ValidationFeature.buildKeyAndErrorMessagesWithPrefix(prefix, validations, errors)
        }
        set(RequestScopeFeature.ATTR_KEY_AND_ERROR_MESSAGES, keyAndErrorMessages)

      }.apply()

    validator
  }

}

object ValidationFeature {

  def buildErrorMessages(validations: Seq[NewValidation], errors: Errors)(
    implicit locale: Locale): Seq[String] = {
    buildErrorMessagesWithPrefix(null, validations, errors)
  }

  def buildErrorMessagesWithPrefix(prefix: String, validations: Seq[NewValidation], errors: Errors)(
    implicit locale: Locale): Seq[String] = {
    val skinnyValidationMessages = Messages.loadFromConfig(locale = Option(locale))
    val i18n = I18n(locale)
    def withPrefix(key: String): String = if (prefix != null) s"${prefix}.${key}" else key

    validations.map(_.paramDef.key).flatMap { key =>
      errors.get(key).map { error =>
        skinnyValidationMessages.get(
          key = error.name,
          params = i18n.get(withPrefix(toCamelCase(key))).getOrElse(key) :: error.messageParams.map {
            case I18nKeyParam(key) => i18n.getOrKey(key)
            case value => value
          }.toList
        ).getOrElse(error.name)
      }
    }
  }

  def buildKeyAndErrorMessages(validations: Seq[NewValidation], errors: Errors)(
    implicit locale: Locale): KeyAndErrorMessages = {
    buildKeyAndErrorMessagesWithPrefix(null, validations, errors)
  }

  def buildKeyAndErrorMessagesWithPrefix(prefix: String, validations: Seq[NewValidation], errors: Errors)(
    implicit locale: Locale): KeyAndErrorMessages = {
    val skinnyValidationMessages = Messages.loadFromConfig(locale = Option(locale))
    val i18n = I18n(locale)
    def withPrefix(key: String): String = if (prefix != null) s"${prefix}.${key}" else key

    KeyAndErrorMessages(validations.map(_.paramDef.key).map { key =>
      key -> errors.get(key).map { error =>
        skinnyValidationMessages.get(
          key = error.name,
          params = i18n.get(withPrefix(toCamelCase(key))).getOrElse(key) :: error.messageParams.map {
            case I18nKeyParam(key) => i18n.getOrKey(key)
            case value => value
          }.toList
        ).getOrElse(error.name)
      }
    }.toMap)
  }

}
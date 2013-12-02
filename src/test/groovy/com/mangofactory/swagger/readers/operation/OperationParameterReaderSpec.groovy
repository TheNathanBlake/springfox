package com.mangofactory.swagger.readers.operation

import com.mangofactory.swagger.configuration.SpringSwaggerModelConfig
import com.mangofactory.swagger.mixins.RequestMappingSupport
import com.mangofactory.swagger.scanners.RequestMappingContext
import com.wordnik.swagger.model.Parameter
import org.springframework.core.MethodParameter
import org.springframework.validation.BindingResult
import org.springframework.web.method.HandlerMethod
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.ServletContext
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static com.mangofactory.swagger.ScalaUtils.toOption

@Mixin(RequestMappingSupport)
class OperationParameterReaderSpec extends Specification {

   @Shared SpringSwaggerModelConfig springSwaggerModelConfig = new SpringSwaggerModelConfig();

   @Unroll
   def "Should ignore ignorables"() {
    given:
      List ignorableParameterTypes = [ServletRequest, ServletResponse, HttpServletRequest, HttpServletResponse, BindingResult, ServletContext]
      RequestMappingContext context = new RequestMappingContext(requestMappingInfo('/somePath'), handlerMethod)
      context.put("ignorableParameterTypes", ignorableParameterTypes as Set)
      context.put("parameterDataTypes", springSwaggerModelConfig.defaultParameterDataTypes())
    when:
      OperationParameterReader operationParameterReader = new OperationParameterReader()
      operationParameterReader.execute(context)
      Map<String, Object> result = context.getResult()

    then:
      result['parameters'].size == expectedSize

    where:
      handlerMethod                                                        | expectedSize
      dummyHandlerMethod('methodWithServletRequest', ServletRequest.class) | 0
      dummyHandlerMethod('methodWithBindingResult', BindingResult.class)   | 0
      dummyHandlerMethod('methodWithInteger', Integer.class)               | 1
   }

   @Unroll
   def "Should read a request mapping method without APIParameter annotation"() {
    given:
      List ignorableParameterTypes = [ServletRequest, ServletResponse, HttpServletRequest, HttpServletResponse, BindingResult, ServletContext]
      HandlerMethod handlerMethod = dummyHandlerMethod('methodWithSinglePathVariable', String.class)

      RequestMappingContext context = new RequestMappingContext(requestMappingInfo('/somePath'), handlerMethod)
      MethodParameter methodParameter = new MethodParameter(handlerMethod.getMethod(), 1)

      context.put("ignorableParameterTypes", ignorableParameterTypes as Set)
      context.put("parameterDataTypes", springSwaggerModelConfig.defaultParameterDataTypes())
      context.put("methodParameter", methodParameter)
    when:
      OperationParameterReader operationParameterReader = new OperationParameterReader()
      operationParameterReader.execute(context)
      Map<String, Object> result = context.getResult()

    then:
      Parameter parameter = result['parameters'][0]
      assert parameter."$property" == expectedValue
    where:
      property        | expectedValue
      'name'          | 'businessId'
      'description'   | toOption('businessId')
      'required'      | true
      'allowMultiple' | false
      'allowMultiple' | false
      'paramType'      | "path"

   }
}

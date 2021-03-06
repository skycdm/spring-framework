/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.util;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.subpackage.Component;
import org.springframework.test.util.subpackage.LegacyEntity;
import org.springframework.test.util.subpackage.Person;
import org.springframework.test.util.subpackage.PersonEntity;
import org.springframework.test.util.subpackage.StaticFields;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.invokeGetterMethod;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;
import static org.springframework.test.util.ReflectionTestUtils.invokeSetterMethod;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * Unit tests for {@link ReflectionTestUtils}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
public class ReflectionTestUtilsTests {

	private static final Float PI = Float.valueOf((float) 22 / 7);

	private final Person person = new PersonEntity();

	private final Component component = new Component();

	private final LegacyEntity entity = new LegacyEntity();


	@Before
	public void resetStaticFields() {
		StaticFields.reset();
	}

	@Test
	public void setFieldWithNullTargetObject() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				setField((Object) null, "id", Long.valueOf(99)))
			.withMessageStartingWith("Either targetObject or targetClass");
	}

	@Test
	public void getFieldWithNullTargetObject() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				getField((Object) null, "id"))
			.withMessageStartingWith("Either targetObject or targetClass");
	}

	@Test
	public void setFieldWithNullTargetClass() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				setField((Class<?>) null, "id", Long.valueOf(99)))
			.withMessageStartingWith("Either targetObject or targetClass");
	}

	@Test
	public void getFieldWithNullTargetClass() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				getField((Class<?>) null, "id"))
			.withMessageStartingWith("Either targetObject or targetClass");
	}

	@Test
	public void setFieldWithNullNameAndNullType() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				setField(person, null, Long.valueOf(99), null))
			.withMessageStartingWith("Either name or type");
	}

	@Test
	public void setFieldWithBogusName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				setField(person, "bogus", Long.valueOf(99), long.class))
			.withMessageStartingWith("Could not find field 'bogus'");
	}

	@Test
	public void setFieldWithWrongType() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				setField(person, "id", Long.valueOf(99), String.class))
			.withMessageStartingWith("Could not find field");
	}

	@Test
	public void setFieldAndGetFieldForStandardUseCases() throws Exception {
		assertSetFieldAndGetFieldBehavior(this.person);
	}

	@Test
	public void setFieldAndGetFieldViaJdkDynamicProxy() throws Exception {
		ProxyFactory pf = new ProxyFactory(this.person);
		pf.addInterface(Person.class);
		Person proxy = (Person) pf.getProxy();
		assertTrue("Proxy is a JDK dynamic proxy", AopUtils.isJdkDynamicProxy(proxy));
		assertSetFieldAndGetFieldBehaviorForProxy(proxy, this.person);
	}

	@Test
	public void setFieldAndGetFieldViaCglibProxy() throws Exception {
		ProxyFactory pf = new ProxyFactory(this.person);
		pf.setProxyTargetClass(true);
		Person proxy = (Person) pf.getProxy();
		assertTrue("Proxy is a CGLIB proxy", AopUtils.isCglibProxy(proxy));
		assertSetFieldAndGetFieldBehaviorForProxy(proxy, this.person);
	}

	private static void assertSetFieldAndGetFieldBehavior(Person person) {
		// Set reflectively
		setField(person, "id", Long.valueOf(99), long.class);
		setField(person, "name", "Tom");
		setField(person, "age", Integer.valueOf(42));
		setField(person, "eyeColor", "blue", String.class);
		setField(person, "likesPets", Boolean.TRUE);
		setField(person, "favoriteNumber", PI, Number.class);

		// Get reflectively
		assertEquals(Long.valueOf(99), getField(person, "id"));
		assertEquals("Tom", getField(person, "name"));
		assertEquals(Integer.valueOf(42), getField(person, "age"));
		assertEquals("blue", getField(person, "eyeColor"));
		assertEquals(Boolean.TRUE, getField(person, "likesPets"));
		assertEquals(PI, getField(person, "favoriteNumber"));

		// Get directly
		assertEquals("ID (private field in a superclass)", 99, person.getId());
		assertEquals("name (protected field)", "Tom", person.getName());
		assertEquals("age (private field)", 42, person.getAge());
		assertEquals("eye color (package private field)", "blue", person.getEyeColor());
		assertEquals("'likes pets' flag (package private boolean field)", true, person.likesPets());
		assertEquals("'favorite number' (package field)", PI, person.getFavoriteNumber());
	}

	private static void assertSetFieldAndGetFieldBehaviorForProxy(Person proxy, Person target) {
		assertSetFieldAndGetFieldBehavior(proxy);

		// Get directly from Target
		assertEquals("ID (private field in a superclass)", 99, target.getId());
		assertEquals("name (protected field)", "Tom", target.getName());
		assertEquals("age (private field)", 42, target.getAge());
		assertEquals("eye color (package private field)", "blue", target.getEyeColor());
		assertEquals("'likes pets' flag (package private boolean field)", true, target.likesPets());
		assertEquals("'favorite number' (package field)", PI, target.getFavoriteNumber());
	}

	@Test
	public void setFieldWithNullValuesForNonPrimitives() throws Exception {
		// Fields must be non-null to start with
		setField(person, "name", "Tom");
		setField(person, "eyeColor", "blue", String.class);
		setField(person, "favoriteNumber", PI, Number.class);
		assertNotNull(person.getName());
		assertNotNull(person.getEyeColor());
		assertNotNull(person.getFavoriteNumber());

		// Set to null
		setField(person, "name", null, String.class);
		setField(person, "eyeColor", null, String.class);
		setField(person, "favoriteNumber", null, Number.class);

		assertNull("name (protected field)", person.getName());
		assertNull("eye color (package private field)", person.getEyeColor());
		assertNull("'favorite number' (package field)", person.getFavoriteNumber());
	}

	@Test
	public void setFieldWithNullValueForPrimitiveLong() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				setField(person, "id", null, long.class));
	}

	@Test
	public void setFieldWithNullValueForPrimitiveInt() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				setField(person, "age", null, int.class));
	}

	@Test
	public void setFieldWithNullValueForPrimitiveBoolean() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				setField(person, "likesPets", null, boolean.class));
	}

	@Test
	public void setStaticFieldViaClass() throws Exception {
		setField(StaticFields.class, "publicField", "xxx");
		setField(StaticFields.class, "privateField", "yyy");

		assertEquals("public static field", "xxx", StaticFields.publicField);
		assertEquals("private static field", "yyy", StaticFields.getPrivateField());
	}

	@Test
	public void setStaticFieldViaClassWithExplicitType() throws Exception {
		setField(StaticFields.class, "publicField", "xxx", String.class);
		setField(StaticFields.class, "privateField", "yyy", String.class);

		assertEquals("public static field", "xxx", StaticFields.publicField);
		assertEquals("private static field", "yyy", StaticFields.getPrivateField());
	}

	@Test
	public void setStaticFieldViaInstance() throws Exception {
		StaticFields staticFields = new StaticFields();
		setField(staticFields, null, "publicField", "xxx", null);
		setField(staticFields, null, "privateField", "yyy", null);

		assertEquals("public static field", "xxx", StaticFields.publicField);
		assertEquals("private static field", "yyy", StaticFields.getPrivateField());
	}

	@Test
	public void getStaticFieldViaClass() throws Exception {
		assertEquals("public static field", "public", getField(StaticFields.class, "publicField"));
		assertEquals("private static field", "private", getField(StaticFields.class, "privateField"));
	}

	@Test
	public void getStaticFieldViaInstance() throws Exception {
		StaticFields staticFields = new StaticFields();
		assertEquals("public static field", "public", getField(staticFields, "publicField"));
		assertEquals("private static field", "private", getField(staticFields, "privateField"));
	}

	@Test
	public void invokeSetterMethodAndInvokeGetterMethodWithExplicitMethodNames() throws Exception {
		invokeSetterMethod(person, "setId", Long.valueOf(1), long.class);
		invokeSetterMethod(person, "setName", "Jerry", String.class);
		invokeSetterMethod(person, "setAge", Integer.valueOf(33), int.class);
		invokeSetterMethod(person, "setEyeColor", "green", String.class);
		invokeSetterMethod(person, "setLikesPets", Boolean.FALSE, boolean.class);
		invokeSetterMethod(person, "setFavoriteNumber", Integer.valueOf(42), Number.class);

		assertEquals("ID (protected method in a superclass)", 1, person.getId());
		assertEquals("name (private method)", "Jerry", person.getName());
		assertEquals("age (protected method)", 33, person.getAge());
		assertEquals("eye color (package private method)", "green", person.getEyeColor());
		assertEquals("'likes pets' flag (protected method for a boolean)", false, person.likesPets());
		assertEquals("'favorite number' (protected method for a Number)", Integer.valueOf(42), person.getFavoriteNumber());

		assertEquals(Long.valueOf(1), invokeGetterMethod(person, "getId"));
		assertEquals("Jerry", invokeGetterMethod(person, "getName"));
		assertEquals(Integer.valueOf(33), invokeGetterMethod(person, "getAge"));
		assertEquals("green", invokeGetterMethod(person, "getEyeColor"));
		assertEquals(Boolean.FALSE, invokeGetterMethod(person, "likesPets"));
		assertEquals(Integer.valueOf(42), invokeGetterMethod(person, "getFavoriteNumber"));
	}

	@Test
	public void invokeSetterMethodAndInvokeGetterMethodWithJavaBeanPropertyNames() throws Exception {
		invokeSetterMethod(person, "id", Long.valueOf(99), long.class);
		invokeSetterMethod(person, "name", "Tom");
		invokeSetterMethod(person, "age", Integer.valueOf(42));
		invokeSetterMethod(person, "eyeColor", "blue", String.class);
		invokeSetterMethod(person, "likesPets", Boolean.TRUE);
		invokeSetterMethod(person, "favoriteNumber", PI, Number.class);

		assertEquals("ID (protected method in a superclass)", 99, person.getId());
		assertEquals("name (private method)", "Tom", person.getName());
		assertEquals("age (protected method)", 42, person.getAge());
		assertEquals("eye color (package private method)", "blue", person.getEyeColor());
		assertEquals("'likes pets' flag (protected method for a boolean)", true, person.likesPets());
		assertEquals("'favorite number' (protected method for a Number)", PI, person.getFavoriteNumber());

		assertEquals(Long.valueOf(99), invokeGetterMethod(person, "id"));
		assertEquals("Tom", invokeGetterMethod(person, "name"));
		assertEquals(Integer.valueOf(42), invokeGetterMethod(person, "age"));
		assertEquals("blue", invokeGetterMethod(person, "eyeColor"));
		assertEquals(Boolean.TRUE, invokeGetterMethod(person, "likesPets"));
		assertEquals(PI, invokeGetterMethod(person, "favoriteNumber"));
	}

	@Test
	public void invokeSetterMethodWithNullValuesForNonPrimitives() throws Exception {
		invokeSetterMethod(person, "name", null, String.class);
		invokeSetterMethod(person, "eyeColor", null, String.class);
		invokeSetterMethod(person, "favoriteNumber", null, Number.class);

		assertNull("name (private method)", person.getName());
		assertNull("eye color (package private method)", person.getEyeColor());
		assertNull("'favorite number' (protected method for a Number)", person.getFavoriteNumber());
	}

	@Test
	public void invokeSetterMethodWithNullValueForPrimitiveLong() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				invokeSetterMethod(person, "id", null, long.class));
	}

	@Test
	public void invokeSetterMethodWithNullValueForPrimitiveInt() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				invokeSetterMethod(person, "age", null, int.class));
	}

	@Test
	public void invokeSetterMethodWithNullValueForPrimitiveBoolean() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				invokeSetterMethod(person, "likesPets", null, boolean.class));
	}

	@Test
	public void invokeMethodWithAutoboxingAndUnboxing() {
		// IntelliJ IDEA 11 won't accept int assignment here
		Integer difference = invokeMethod(component, "subtract", 5, 2);
		assertEquals("subtract(5, 2)", 3, difference.intValue());
	}

	@Test
	@Ignore("[SPR-8644] findMethod() does not currently support var-args")
	public void invokeMethodWithPrimitiveVarArgs() {
		// IntelliJ IDEA 11 won't accept int assignment here
		Integer sum = invokeMethod(component, "add", 1, 2, 3, 4);
		assertEquals("add(1,2,3,4)", 10, sum.intValue());
	}

	@Test
	public void invokeMethodWithPrimitiveVarArgsAsSingleArgument() {
		// IntelliJ IDEA 11 won't accept int assignment here
		Integer sum = invokeMethod(component, "add", new int[] { 1, 2, 3, 4 });
		assertEquals("add(1,2,3,4)", 10, sum.intValue());
	}

	@Test
	public void invokeMethodSimulatingLifecycleEvents() {
		assertNull("number", component.getNumber());
		assertNull("text", component.getText());

		// Simulate autowiring a configuration method
		invokeMethod(component, "configure", Integer.valueOf(42), "enigma");
		assertEquals("number should have been configured", Integer.valueOf(42), component.getNumber());
		assertEquals("text should have been configured", "enigma", component.getText());

		// Simulate @PostConstruct life-cycle event
		invokeMethod(component, "init");
		// assertions in init() should succeed

		// Simulate @PreDestroy life-cycle event
		invokeMethod(component, "destroy");
		assertNull("number", component.getNumber());
		assertNull("text", component.getText());
	}

	@Test
	public void invokeInitMethodBeforeAutowiring() {
		assertThatIllegalStateException().isThrownBy(() ->
				invokeMethod(component, "init"))
			.withMessageStartingWith("number must not be null");
	}

	@Test
	public void invokeMethodWithIncompatibleArgumentTypes() {
		assertThatIllegalStateException().isThrownBy(() ->
				invokeMethod(component, "subtract", "foo", 2.0))
		.withMessageStartingWith("Method not found");
	}

	@Test
	public void invokeMethodWithTooFewArguments() {
		assertThatIllegalStateException().isThrownBy(() ->
				invokeMethod(component, "configure", Integer.valueOf(42)))
			.withMessageStartingWith("Method not found");
	}

	@Test
	public void invokeMethodWithTooManyArguments() {
		assertThatIllegalStateException().isThrownBy(() ->
				invokeMethod(component, "configure", Integer.valueOf(42), "enigma", "baz", "quux"))
			.withMessageStartingWith("Method not found");
	}

	@Test // SPR-14363
	public void getFieldOnLegacyEntityWithSideEffectsInToString() {
		Object collaborator = getField(entity, "collaborator");
		assertNotNull(collaborator);
	}

	@Test // SPR-9571 and SPR-14363
	public void setFieldOnLegacyEntityWithSideEffectsInToString() {
		String testCollaborator = "test collaborator";
		setField(entity, "collaborator", testCollaborator, Object.class);
		assertTrue(entity.toString().contains(testCollaborator));
	}

	@Test // SPR-14363
	public void invokeMethodOnLegacyEntityWithSideEffectsInToString() {
		invokeMethod(entity, "configure", Integer.valueOf(42), "enigma");
		assertEquals("number should have been configured", Integer.valueOf(42), entity.getNumber());
		assertEquals("text should have been configured", "enigma", entity.getText());
	}

	@Test // SPR-14363
	public void invokeGetterMethodOnLegacyEntityWithSideEffectsInToString() {
		Object collaborator = invokeGetterMethod(entity, "collaborator");
		assertNotNull(collaborator);
	}

	@Test // SPR-14363
	public void invokeSetterMethodOnLegacyEntityWithSideEffectsInToString() {
		String testCollaborator = "test collaborator";
		invokeSetterMethod(entity, "collaborator", testCollaborator);
		assertTrue(entity.toString().contains(testCollaborator));
	}

}

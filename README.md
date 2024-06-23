# Query Parser


```xml
<depedency>
	<groupId>am.ik.query</groupId>
	<artifactId>query-parser</artifactId>
	<version>0.1.0</version>
</depedency>
```


```java
RootNode node = QueryParser.parseQuery("hello (world or java)");
Node.print(node);
//	TokenNode[type=KEYWORD, value=hello]
//	RootNode
//		TokenNode[type=KEYWORD, value=world]
//		TokenNode[type=OR, value=or]
//		TokenNode[type=KEYWORD, value=java]
```
spray-multi-params-test
=======================

Trying out extraction of multi params using Spray's built-in directives.

This request:

```
GET /?name=Bob&hobbies=sailing&hobbies=surfing&hobbies=watching%20movies
```

in this route

```scala
parameters('name.as[String], 'hobbies.as[List[String]]) { (name, hobbies) =>
  complete {
    s"$name likes ${hobbies dropRight 1 mkString ", "} and ${hobbies.last}."
  }
}
```

returns

```
Bob likes sailing, surfing and watching movies.
```


interface Expression{

}

class OpJsonSerializer extends JsonSerializer<ComplexExpression> {


    @Override
    public void serialize(ComplexExpression complexExpression, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
//        jgen.writeStartObject();
//        jgen.writeNumberField("id", value.id);
//        jgen.writeStringField("itemName", value.itemName);
//        jgen.writeNumberField("owner", value.owner.id);
//        jgen.writeEndObject();
        jsonGenerator.writeStartObject();
        jsonGenerator.writeArrayFieldStart(complexExpression.getOp());
        for (Expression expression : complexExpression.getChildExpression()) {
            jsonGenerator.writeObject(expression);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}

@Data
class SimpleExpression implements Expression{
    private String left;
    private String op;
    private String right;

    public SimpleExpression(String left, String op, String right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }
}

@JsonSerialize(using = OpJsonSerializer.class)
@Data
class ComplexExpression implements Expression{

     private List<Expression> childExpression;

     private String op;
}


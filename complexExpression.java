
sealed interface Expression permits ComplexExpression, SimpleExpression {

}

class OpJsonSerializer extends JsonSerializer<ComplexExpression> {


    @Override
    public void serialize(ComplexExpression complexExpression, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeArrayFieldStart(complexExpression.getOp());
        for (Expression expression : complexExpression.getChildExpression()) {
            jsonGenerator.writeObject(expression);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}

final class SimpleExpression implements Expression {
    private String left;
    private String op;
    private String right;

    public SimpleExpression(String left, String op, String right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = right;
    }
}

@JsonSerialize(using = OpJsonSerializer.class)
final class ComplexExpression implements Expression {

    private List<Expression> childExpression;

    private String op;

    public List<Expression> getChildExpression() {
        return childExpression;
    }

    public void setChildExpression(List<Expression> childExpression) {
        this.childExpression = childExpression;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }
}


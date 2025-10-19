import subprocess, json

JAR_NAME = 'ga_opt/target/optimizer-1.0.jar'

def call_java_optimizer(params, jar_path=JAR_NAME):
    process = subprocess.Popen(
        ['java', '-jar', jar_path],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )

    input_json = json.dumps(params)
    stdout, stderr = process.communicate(input_json)

    if stderr:
        print("Java error:", stderr)
    return stdout
    return json.loads(stdout)

if __name__ == "__main__":
    out = call_java_optimizer(
                {
              "itemTypes": [
                {"number": 1, "width": 5.0, "height": 3.0, "price": 25.0, "quantity": 20},
                {"number": 2, "width": 10.0, "height": 15.0, "price": 55.0, "quantity": 10},
                {"number": 3, "width": 15.0, "height": 10.0, "price": 45.0, "quantity": 15},
                {"number": 4, "width": 25.0, "height": 20.0, "price": 75.0, "quantity": 5}
              ],
              "binTypes": [
                {"number": 1, "width": 120.0, "height": 30.0},
                {"number": 2, "width": 500.0, "height": 50.0},
                {"number": 3, "width": 100.0, "height": 100.0},
                {"number": 4, "width": 200.0, "height": 200.0}
              ]
            }
    )
    print(out)